package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
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

// CacheClient.java 是缓存访问的统一工具类，封装 Redis 缓存读取、回源和重建策略。
// 封装 Redis 缓存访问逻辑，统一处理缓存读取、数据库回源、空值缓存、逻辑过期、异步重建、冷 miss 并发保护。
// 当前 CacheClient 对缓存三大问题都有处理：
// 通过空值缓存解决缓存穿透；通过逻辑过期、互斥锁、异步重建解决热点 key 的缓存击穿；
// 通过逻辑过期缓存不设置物理 TTL、逻辑过期时间增加随机抖动，部分缓解缓存雪崩。
// 但它不能完整解决 Redis 宕机、批量 key 冷 miss、大量不同 key 同时失效等雪崩场景。
// 所以更准确的说法是：解决了穿透和击穿，缓解了雪崩。
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
     * 将任意对象序列化成json存入redis
     *
     * @param key   关键
     * @param value 价值
     * @param time  时间
     * @param unit  单位
     */
    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    /**
     * 将任意对象序列化成json存入redis 并且携带逻辑过期时间
     *
     * @param key   关键
     * @param value 价值
     * @param time  时间
     * @param unit  单位
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
     * 设置空值解决缓存穿透
     *
     * @param keyPrefix  关键前缀
     * @param id         id
     * @param type       类型
     * @param dbFallback db回退
     * @param time       时间
     * @param unit       单位
     * @return {@link R}
     */
    public <R, ID> R queryWithPassThrough(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        // 从redis中查询
        String json = stringRedisTemplate.opsForValue().get(key);
        // 判断是否存在
        if (StringUtils.isNotEmpty(json)) {
            // 存在直接返回
            return JSONUtil.toBean(json, type);
        }
        // 判断空值
        if ("".equals(json)) {
            return null;
        }
        // 不存在 查询数据库
        R r = dbFallback.apply(id);
        if (r == null) {
            // redis写入空值
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.SECONDS);
            // 数据库不存在 返回错误
            return null;
        }
        // 数据库存在 写入redis
        this.set(key, r, time, unit);
        // 返回
        return r;
    }

    /**
     * 逻辑过期解决缓存击穿
     *
     * @param id id
     * @return 缓存或数据库中的业务对象
     */
    public <R, ID> R queryWithLogicalExpire(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback,
            Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        CacheLookup<R> lookup = readLogicalCache(key, type, time, unit);
        if (lookup.isMiss()) {
            return queryColdMiss(key, id, type, dbFallback, time, unit);
        }
        if (lookup.isInvalid()) {
            stringRedisTemplate.delete(key);
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
                if (lookup.isInvalid()) {
                    stringRedisTemplate.delete(key);
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
            if (lookup.isInvalid()) {
                stringRedisTemplate.delete(key);
                log.debug("Logical cache cold miss retry found invalid cache, stop retrying: cacheKey={}, retry={}",
                        key, retry);
                break;
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
            return CacheLookup.invalid();
        }

        LocalDateTime expireTime = redisData.getExpireTime();
        if (expireTime == null) {
            try {
                R r = JSONUtil.toBean(json, type);
                setWithLogicalExpire(key, r, time, unit);
                log.debug("Migrated legacy cache to logical expire format: cacheKey={}", key);
                return CacheLookup.hit(r);
            } catch (Exception e) {
                log.warn("Invalid legacy cache data, fallback to DB: cacheKey={}", key, e);
                return CacheLookup.invalid();
            }
        }

        Object data = redisData.getData();
        if (data == null) {
            log.warn("Invalid logical cache payload, fallback to DB: cacheKey={}", key);
            return CacheLookup.invalid();
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
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.SECONDS);
            log.debug("Set empty cache after DB miss: cacheKey={}, ttlSeconds={}", key, CACHE_NULL_TTL);
            return null;
        }
        setWithLogicalExpire(key, r, time, unit);
        log.debug("Loaded logical cache from DB: cacheKey={}", key);
        return r;
    }

    /**
     * 获取锁
     *
     * @param key 关键
     * @return boolean
     */
    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    /**
     * 释放锁
     *
     * @param key 关键
     */
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
                    stringRedisTemplate.opsForValue().set(cacheKey, "", CACHE_NULL_TTL, TimeUnit.SECONDS);
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
        EXPIRED,
        INVALID
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

        private static <R> CacheLookup<R> invalid() {
            return new CacheLookup<>(CacheStatus.INVALID, null);
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

        private boolean isInvalid() {
            return status == CacheStatus.INVALID;
        }

        private boolean isResolved() {
            return status == CacheStatus.EMPTY || status == CacheStatus.HIT || status == CacheStatus.EXPIRED;
        }
    }
}
