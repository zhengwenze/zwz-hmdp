package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.hmdp.utils.RedisConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 商铺类型查询实现类
 */
@Slf4j
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    private static final long CACHE_REBUILD_RETRY_INTERVAL_MILLIS = 50L;
    private static final int CACHE_REBUILD_RETRY_TIMES = 3;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result getTypeList() {
        List<ShopType> typeList = readTypeCache();
        if (typeList != null) {
            return Result.ok(typeList);
        }

        boolean locked = tryLock();
        if (!locked) {
            return retryReadTypeCache();
        }

        try {
            typeList = readTypeCache();
            if (typeList != null) {
                return Result.ok(typeList);
            }
            return Result.ok(loadTypeListFromDb());
        } finally {
            unlock();
        }
    }

    @Override
    public boolean save(ShopType entity) {
        boolean saved = super.save(entity);
        if (saved) {
            clearTypeCache();
        }
        return saved;
    }

    @Override
    public boolean updateById(ShopType entity) {
        boolean updated = super.updateById(entity);
        if (updated) {
            clearTypeCache();
        }
        return updated;
    }

    @Override
    public boolean removeById(Serializable id) {
        boolean removed = super.removeById(id);
        if (removed) {
            clearTypeCache();
        }
        return removed;
    }

    private List<ShopType> readTypeCache() {
        String json;
        try {
            json = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_TYPE_KEY);
        } catch (Exception e) {
            log.warn("Read shop type cache failed, delete stale cache key: {}", RedisConstants.CACHE_TYPE_KEY, e);
            stringRedisTemplate.delete(RedisConstants.CACHE_TYPE_KEY);
            return null;
        }
        if (json == null) {
            return null;
        }
        return JSONUtil.toList(json, ShopType.class);
    }

    private Result retryReadTypeCache() {
        for (int i = 0; i < CACHE_REBUILD_RETRY_TIMES; i++) {
            if (!sleepBeforeRetry()) {
                break;
            }
            List<ShopType> typeList = readTypeCache();
            if (typeList != null) {
                return Result.ok(typeList);
            }
        }
        return Result.fail("商铺类型缓存正在重建，请稍后重试");
    }

    private List<ShopType> loadTypeListFromDb() {
        List<ShopType> typeList = query().orderByAsc("sort").list();
        if (typeList == null || typeList.isEmpty()) {
            typeList = Collections.emptyList();
            writeTypeCache(typeList, RedisConstants.CACHE_TYPE_NULL_TTL);
            return typeList;
        }
        writeTypeCache(typeList, RedisConstants.CACHE_TYPE_TTL);
        return typeList;
    }

    private void writeTypeCache(List<ShopType> typeList, Long ttlMinutes) {
        stringRedisTemplate.opsForValue().set(
                RedisConstants.CACHE_TYPE_KEY,
                JSONUtil.toJsonStr(typeList),
                ttlMinutes,
                TimeUnit.MINUTES);
    }

    private boolean tryLock() {
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(
                RedisConstants.LOCK_TYPE_KEY,
                "1",
                RedisConstants.LOCK_TYPE_TTL,
                TimeUnit.SECONDS);
        return Boolean.TRUE.equals(locked);
    }

    private void unlock() {
        stringRedisTemplate.delete(RedisConstants.LOCK_TYPE_KEY);
    }

    private boolean sleepBeforeRetry() {
        try {
            Thread.sleep(CACHE_REBUILD_RETRY_INTERVAL_MILLIS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void clearTypeCache() {
        stringRedisTemplate.delete(RedisConstants.CACHE_TYPE_KEY);
    }
}
