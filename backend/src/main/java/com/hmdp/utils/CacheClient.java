package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.config.AsyncExecutorConfig.CacheRebuildTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 * 缓存访问统一工具类，封装 Redis 缓存读取、数据库回源和缓存重建策略。
 * </p>
 *
 * <h3>核心功能</h3>
 * <ul>
 * <li><strong>缓存穿透防护</strong>：通过空值缓存（缓存 null）避免大量请求查询不存在的数据</li>
 * <li><strong>缓存击穿防护</strong>：通过逻辑过期 + 互斥锁 + 异步重建解决热点 Key 失效问题</li>
 * <li><strong>缓存雪崩缓解</strong>：通过逻辑过期时间随机抖动避免大量 Key 同时失效</li>
 * <li><strong>冷启动保护</strong>：通过互斥锁 + 双重检查 + 重试机制防止并发回源数据库</li>
 * </ul>
 *
 * <h3>缓存策略说明</h3>
 * <ul>
 * <li><strong>逻辑过期</strong>：不设置物理 TTL，在数据中存储逻辑过期时间，避免缓存失效瞬间的高并发</li>
 * <li><strong>异步重建</strong>：缓存过期后，由获取到锁的线程异步重建，其他线程返回旧数据</li>
 * <li><strong>随机抖动</strong>：过期时间增加 60-600 秒随机抖动，避免雪崩（缓解非解决）</li>
 * </ul>
 *
 * <h3>局限性说明</h3>
 * <p>
 * 本工具类解决了缓存穿透和缓存击穿问题，缓解了缓存雪崩问题。但以下场景仍需注意：
 * </p>
 * <ul>
 * <li>Redis 宕机导致的缓存不可用</li>
 * <li>批量不同 Key 冷启动导致的并发回源</li>
 * <li>大量不同 Key 同时失效导致的雪崩</li>
 * </ul>
 */
@Slf4j
@Component
public class CacheClient {
    private static final long COLD_MISS_RETRY_INTERVAL_MILLIS = 50L;
    private static final int COLD_MISS_RETRY_TIMES = 3;
    private static final double LOGICAL_EXPIRE_JITTER_RATIO = 0.1D;
    private static final long LOGICAL_EXPIRE_MIN_JITTER_SECONDS = 60L;
    private static final long LOGICAL_EXPIRE_MAX_JITTER_SECONDS = 600L;

    private final StringRedisTemplate stringRedisTemplate;
    private final ThreadPoolTaskExecutor cacheRebuildExecutor;

    @Autowired
    public CacheClient(StringRedisTemplate stringRedisTemplate,
            @Qualifier("cacheRebuildExecutor") ThreadPoolTaskExecutor cacheRebuildExecutor) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.cacheRebuildExecutor = cacheRebuildExecutor;
    }

    /**
     * 将对象序列化为 JSON 字符串并存储到 Redis。
     *
     * @param key   Redis 键
     * @param value 要存储的对象（任意类型）
     * @param time  过期时间
     * @param unit  时间单位
     */
    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    /**
     * 将对象序列化为 JSON 并存储到 Redis，同时设置逻辑过期时间。
     * <p>
     * 逻辑过期特点：
     * <ul>
     * <li>不设置物理 TTL，缓存永不过期</li>
     * <li>在数据中存储逻辑过期时间，由应用层判断是否过期</li>
     * <li>过期时间增加随机抖动（60-600 秒），缓解缓存雪崩</li>
     * </ul>
     *
     * @param key   Redis 键
     * @param value 要存储的对象（任意类型）
     * @param time  基础过期时间
     * @param unit  时间单位
     */
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        // 封装逻辑过期时间
        long baseTtlSeconds = unit.toSeconds(time);
        long jitterBoundSeconds = calculateJitterBoundSeconds(baseTtlSeconds);
        long jitterSeconds = ThreadLocalRandom.current().nextLong(1, jitterBoundSeconds + 1);
        LocalDateTime expireTime = LocalDateTime.now()
                .plusSeconds(baseTtlSeconds)
                .plusSeconds(jitterSeconds);
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(expireTime);
        // 存入redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
        log.debug("Set logical cache: cacheKey={}, baseTtlSeconds={}, jitterSeconds={}, expireTime={}",
                key, baseTtlSeconds, jitterSeconds, expireTime);
    }

    /**
     * 查询数据，使用缓存穿透防护策略。
     * <p>
     * 工作流程：
     * <ol>
     * <li>查询 Redis 缓存，命中则直接返回</li>
     * <li>命中空值缓存（""），返回 null</li>
     * <li>缓存未命中，查询数据库</li>
     * <li>数据库不存在，写入空值缓存（防止穿透）</li>
     * <li>数据库存在，写入 Redis 缓存</li>
     * </ol>
     *
     * @param keyPrefix  Redis 键前缀（如 "cache:shop:"）
     * @param id         业务 ID
     * @param type       目标类型
     * @param dbFallback 数据库回源函数（如 this::getById）
     * @param time       缓存过期时间
     * @param unit       时间单位
     * @param <R>        返回类型
     * @param <ID>       ID 类型
     * @return 缓存或数据库中的业务对象，不存在则返回 null
     */
    public <R, ID> R queryWithPassThrough(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        // 从redis中查询
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json != null) {
            return json.isEmpty() ? null : JSONUtil.toBean(json, type);
        }
        // 不存在 查询数据库
        R r = dbFallback.apply(id);
        if (r == null) {
            // redis写入空值
            setEmptyCache(key);
            // 数据库不存在 返回错误
            return null;
        }
        // 数据库存在 写入redis
        this.set(key, r, time, unit);
        // 返回
        return r;
    }

    /**
     * 查询数据，使用逻辑过期解决缓存击穿问题。
     * <p>
     * 工作流程：
     * <ol>
     * <li>查询 Redis 缓存，解析逻辑过期时间</li>
     * <li>缓存未命中（冷启动），使用互斥锁 + 双重检查 + 重试机制保护</li>
     * <li>缓存格式无效，删除后重新加载</li>
     * <li>命中空值缓存，返回 null</li>
     * <li>缓存命中且未过期，直接返回</li>
     * <li>缓存已过期：
     * <ul>
     * <li>获取互斥锁成功：提交异步重建任务，返回旧数据</li>
     * <li>获取互斥锁失败：跳过重建，返回旧数据</li>
     * </ul>
     * </li>
     * </ol>
     *
     * @param keyPrefix  Redis 键前缀（如 "cache:shop:"）
     * @param id         业务 ID
     * @param type       目标类型
     * @param dbFallback 数据库回源函数（如 this::getById）
     * @param time       基础过期时间
     * @param unit       时间单位
     * @param <R>        返回类型
     * @param <ID>       ID 类型
     * @return 缓存或数据库中的业务对象
     */
    public <R, ID> R queryWithLogicalExpire(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback,
            Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        CacheLookup<R> lookup = readLogicalCache(key, type, time, unit);
        if (lookup.isMiss()) {
            return queryColdMiss(key, id, type, dbFallback, time, unit);
        }
        if (lookup.isEmpty()) {
            return null;
        }
        if (lookup.isHit()) {
            return lookup.value;
        }

        // 已过期
        // 获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        boolean flag = tryLock(lockKey);
        // 是否获取锁成功
        if (flag) {
            // 成功 异步重建
            log.debug("Logical cache expired, submit async rebuild: cacheKey={}, lockKey={}", key, lockKey);
            cacheRebuildExecutor.execute(new CacheRebuildRunnable<>(key, lockKey, id, dbFallback, time, unit));
        } else {
            log.debug("Logical cache expired, rebuild skipped because lock is held: cacheKey={}, lockKey={}", key,
                    lockKey);
        }
        // 返回过期商铺信息
        return lookup.value;
    }

    private <R, ID> R queryColdMiss(String key, ID id, Class<R> type, Function<ID, R> dbFallback, Long time,
            TimeUnit unit) {
        String lockKey = LOCK_SHOP_KEY + id;
        log.debug("Logical cache cold miss: cacheKey={}, lockKey={}", key, lockKey);

        boolean locked = tryLock(lockKey);
        if (locked) {
            try {
                CacheLookup<R> lookup = readLogicalCache(key, type, time, unit);
                if (lookup.isResolved()) {
                    log.debug("Logical cache cold miss double check resolved: cacheKey={}, status={}", key,
                            lookup.status);
                    return lookup.value;
                }
                return loadAndSetLogicalExpire(key, id, dbFallback, time, unit);
            } finally {
                unLock(lockKey);
            }
        }

        log.debug("Logical cache cold miss lock failed, retry with limited waits: cacheKey={}, lockKey={}", key,
                lockKey);

        for (int retry = 1; retry <= COLD_MISS_RETRY_TIMES; retry++) {
            if (!sleepBeforeColdMissRetry(key, retry)) {
                break;
            }

            CacheLookup<R> lookup = readLogicalCache(key, type, time, unit);
            if (lookup.isResolved()) {
                log.debug("Logical cache cold miss retry resolved: cacheKey={}, retry={}, status={}", key, retry,
                        lookup.status);
                return lookup.value;
            }
        }

        log.debug("Logical cache cold miss retry exhausted, fallback to DB once: cacheKey={}", key);
        return loadAndSetLogicalExpire(key, id, dbFallback, time, unit);
    }

    private boolean sleepBeforeColdMissRetry(String key, int retry) {
        try {
            Thread.sleep(COLD_MISS_RETRY_INTERVAL_MILLIS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.debug("Logical cache cold miss retry interrupted: cacheKey={}, retry={}", key, retry);
            return false;
        }
    }

    private long calculateJitterBoundSeconds(long baseTtlSeconds) {
        long ratioJitterSeconds = Math.round(baseTtlSeconds * LOGICAL_EXPIRE_JITTER_RATIO);
        return Math.min(LOGICAL_EXPIRE_MAX_JITTER_SECONDS,
                Math.max(LOGICAL_EXPIRE_MIN_JITTER_SECONDS, ratioJitterSeconds));
    }

    private <R> CacheLookup<R> readLogicalCache(String key, Class<R> type, Long time, TimeUnit unit) {
        String json = stringRedisTemplate.opsForValue().get(key);
        return parseLogicalCache(key, json, type, time, unit);
    }

    private <R> CacheLookup<R> parseLogicalCache(String key, String json, Class<R> type, Long time, TimeUnit unit) {
        if (json == null) {
            return CacheLookup.miss();
        }
        if ("".equals(json)) {
            return CacheLookup.empty();
        }

        RedisData redisData;
        try {
            redisData = JSONUtil.toBean(json, RedisData.class);
        } catch (Exception e) {
            log.warn("Invalid logical cache data, fallback to DB: cacheKey={}", key, e);
            return CacheLookup.miss();
        }

        LocalDateTime expireTime = redisData.getExpireTime();

        Object data = redisData.getData();
        if (data == null) {
            log.warn("Invalid logical cache payload, fallback to DB: cacheKey={}", key);
            return CacheLookup.miss();
        }

        R r = BeanUtil.toBean(data, type);
        if (expireTime.isAfter(LocalDateTime.now())) {
            return CacheLookup.hit(r);
        }
        return CacheLookup.expired(r);
    }

    private <R, ID> R loadAndSetLogicalExpire(String key, ID id, Function<ID, R> dbFallback, Long time,
            TimeUnit unit) {
        R r = dbFallback.apply(id);
        if (r == null) {
            setEmptyCache(key);
            log.debug("Set empty cache after DB miss: cacheKey={}, ttlSeconds={}", key, CACHE_NULL_TTL);
            return null;
        }
        setWithLogicalExpire(key, r, time, unit);
        log.debug("Loaded logical cache from DB: cacheKey={}", key);
        return r;
    }

    /**
     * 获取互斥锁（基于 Redis SETNX）。
     *
     * @param key 锁的 Redis 键
     * @return 获取锁是否成功
     */
    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void setEmptyCache(String key) {
        stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.SECONDS);
    }

    private void unLock(String key) {
        stringRedisTemplate.delete(key);
    }

    private final class CacheRebuildRunnable<ID, R> implements CacheRebuildTask {
        private final String cacheKey;
        private final String lockKey;
        private final ID id;
        private final Function<ID, R> dbFallback;
        private final Long time;
        private final TimeUnit unit;

        private CacheRebuildRunnable(String cacheKey, String lockKey, ID id, Function<ID, R> dbFallback, Long time,
                TimeUnit unit) {
            this.cacheKey = cacheKey;
            this.lockKey = lockKey;
            this.id = id;
            this.dbFallback = dbFallback;
            this.time = time;
            this.unit = unit;
        }

        @Override
        public String getCacheKey() {
            return cacheKey;
        }

        @Override
        public void onRejected() {
            unLock(lockKey);
        }

        @Override
        public void run() {
            try {
                R newR = dbFallback.apply(id);
                if (newR == null) {
                    setEmptyCache(cacheKey);
                    log.debug("Cache rebuild completed with empty value: cacheKey={}", cacheKey);
                } else {
                    setWithLogicalExpire(cacheKey, newR, time, unit);
                    log.debug("Cache rebuild completed: cacheKey={}", cacheKey);
                }
            } catch (Exception e) {
                log.warn("Cache rebuild task failed: cacheKey={}, lockKey={}", cacheKey, lockKey, e);
            } finally {
                unLock(lockKey);
            }
        }
    }

    private enum CacheStatus {
        MISS,
        EMPTY,
        HIT,
        EXPIRED
    }

    private static final class CacheLookup<R> {
        private final CacheStatus status;
        private final R value;

        private CacheLookup(CacheStatus status, R value) {
            this.status = status;
            this.value = value;
        }

        private static <R> CacheLookup<R> miss() {
            return new CacheLookup<>(CacheStatus.MISS, null);
        }

        private static <R> CacheLookup<R> empty() {
            return new CacheLookup<>(CacheStatus.EMPTY, null);
        }

        private static <R> CacheLookup<R> hit(R value) {
            return new CacheLookup<>(CacheStatus.HIT, value);
        }

        private static <R> CacheLookup<R> expired(R value) {
            return new CacheLookup<>(CacheStatus.EXPIRED, value);
        }

        private boolean isMiss() {
            return status == CacheStatus.MISS;
        }

        private boolean isEmpty() {
            return status == CacheStatus.EMPTY;
        }

        private boolean isHit() {
            return status == CacheStatus.HIT;
        }

        private boolean isResolved() {
            return status == CacheStatus.EMPTY || status == CacheStatus.HIT || status == CacheStatus.EXPIRED;
        }
    }
}
