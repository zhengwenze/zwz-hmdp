package com.hmdp.utils;

import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.CACHE_NULL_TTL;
import static com.hmdp.utils.RedisConstants.LOCK_SHOP_KEY;
import static com.hmdp.utils.RedisConstants.LOCK_SHOP_TTL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CacheClientTest {

    private CacheClient cacheClient;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ThreadPoolTaskExecutor cacheRebuildExecutor;

    @Mock
    private Function<Long, Shop> dbFallback;

    @BeforeEach
    void setUp() {
        cacheClient = new CacheClient(stringRedisTemplate, cacheRebuildExecutor);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void queryWithPassThrough_shouldReturnCachedValueWhenCacheHits() {
        Shop cached = shop(1L, "cached-shop");
        when(valueOperations.get("cache:shop:1")).thenReturn(JSONUtil.toJsonStr(cached));

        Shop result = cacheClient.queryWithPassThrough("cache:shop:", 1L, Shop.class, dbFallback, 30L,
                TimeUnit.MINUTES);

        assertEquals(1L, result.getId());
        assertEquals("cached-shop", result.getName());
        verify(dbFallback, never()).apply(any());
    }

    @Test
    void queryWithPassThrough_shouldLoadDatabaseAndWriteRedisWhenCacheMisses() {
        Shop loaded = shop(1L, "db-shop");
        when(valueOperations.get("cache:shop:1")).thenReturn(null);
        when(dbFallback.apply(1L)).thenReturn(loaded);

        Shop result = cacheClient.queryWithPassThrough("cache:shop:", 1L, Shop.class, dbFallback, 30L,
                TimeUnit.MINUTES);

        assertSame(loaded, result);
        verify(valueOperations).set(eq("cache:shop:1"), anyString(), eq(30L), eq(TimeUnit.MINUTES));
    }

    @Test
    void queryWithPassThrough_shouldWriteEmptyCacheWhenDatabaseReturnsNull() {
        when(valueOperations.get("cache:shop:1")).thenReturn(null);
        when(dbFallback.apply(1L)).thenReturn(null);

        Shop result = cacheClient.queryWithPassThrough("cache:shop:", 1L, Shop.class, dbFallback, 30L,
                TimeUnit.MINUTES);

        assertNull(result);
        verify(valueOperations).set("cache:shop:1", "", CACHE_NULL_TTL, TimeUnit.SECONDS);
    }

    @Test
    void queryWithPassThrough_shouldReturnNullWhenEmptyCacheHits() {
        when(valueOperations.get("cache:shop:1")).thenReturn("");

        Shop result = cacheClient.queryWithPassThrough("cache:shop:", 1L, Shop.class, dbFallback, 30L,
                TimeUnit.MINUTES);

        assertNull(result);
        verify(dbFallback, never()).apply(any());
    }

    @Test
    void queryWithLogicalExpire_shouldReturnCachedValueWhenLogicalCacheIsFresh() {
        Shop cached = shop(1L, "fresh-shop");
        when(valueOperations.get("cache:shop:1"))
                .thenReturn(logicalCache(cached, LocalDateTime.now().plusMinutes(5)));

        Shop result = cacheClient.queryWithLogicalExpire("cache:shop:", 1L, Shop.class, dbFallback, 30L,
                TimeUnit.MINUTES);

        assertEquals("fresh-shop", result.getName());
        verify(dbFallback, never()).apply(any());
        verify(cacheRebuildExecutor, never()).execute(any(Runnable.class));
    }

    @Test
    void queryWithLogicalExpire_shouldReturnOldValueAndSubmitRebuildWhenLogicalCacheExpired() {
        Shop cached = shop(1L, "expired-shop");
        when(valueOperations.get("cache:shop:1"))
                .thenReturn(logicalCache(cached, LocalDateTime.now().minusMinutes(1)));
        when(valueOperations.setIfAbsent(LOCK_SHOP_KEY + 1L, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS))
                .thenReturn(true);

        Shop result = cacheClient.queryWithLogicalExpire("cache:shop:", 1L, Shop.class, dbFallback, 30L,
                TimeUnit.MINUTES);

        assertEquals("expired-shop", result.getName());
        verify(cacheRebuildExecutor).execute(any(Runnable.class));
    }

    @Test
    void queryWithLogicalExpire_shouldLoadDatabaseWhenLogicalCacheKeyIsMissing() {
        Shop loaded = shop(1L, "db-shop");
        when(valueOperations.get("cache:shop:1")).thenReturn(null, null);
        when(valueOperations.setIfAbsent(LOCK_SHOP_KEY + 1L, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS))
                .thenReturn(true);
        when(dbFallback.apply(1L)).thenReturn(loaded);

        Shop result = cacheClient.queryWithLogicalExpire("cache:shop:", 1L, Shop.class, dbFallback, 30L,
                TimeUnit.MINUTES);

        assertSame(loaded, result);
        verify(dbFallback).apply(1L);
        verify(valueOperations).set(eq("cache:shop:1"), anyString());
        verify(stringRedisTemplate).delete(LOCK_SHOP_KEY + 1L);
    }

    private Shop shop(Long id, String name) {
        Shop shop = new Shop();
        shop.setId(id);
        shop.setName(name);
        return shop;
    }

    private String logicalCache(Shop shop, LocalDateTime expireTime) {
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(expireTime);
        return JSONUtil.toJsonStr(redisData);
    }
}
