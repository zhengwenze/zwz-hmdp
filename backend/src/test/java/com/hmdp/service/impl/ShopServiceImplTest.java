package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.dto.ShopCreateRequest;
import com.hmdp.entity.Shop;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TTL;
import static com.hmdp.utils.RedisConstants.SHOP_GEO_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShopServiceImplTest {

    @Spy
    private ShopServiceImpl shopService;

    @Mock
    private ShopMapper shopMapper;

    @Mock
    private ShopTypeMapper shopTypeMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private CacheClient cacheClient;

    @Mock
    private LambdaQueryChainWrapper<Shop> queryChainWrapper;

    @Mock
    private GeoOperations<String, String> geoOperations;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(shopService, "baseMapper", shopMapper);
        ReflectionTestUtils.setField(shopService, "shopTypeMapper", shopTypeMapper);
        ReflectionTestUtils.setField(shopService, "stringRedisTemplate", stringRedisTemplate);
        ReflectionTestUtils.setField(shopService, "cacheClient", cacheClient);
        lenient().doReturn(queryChainWrapper).when(shopService).lambdaQuery();
    }

    @Test
    void queryById_shouldReturnShopFromCacheClient() {
        Shop shop = shop(1L, "cached-shop");
        doReturn(shop).when(cacheClient).queryWithLogicalExpire(eq(CACHE_SHOP_KEY), eq(1L), eq(Shop.class),
                any(Function.class), eq(CACHE_SHOP_TTL), eq(TimeUnit.MINUTES));

        Result result = shopService.queryById(1L);

        assertTrue(result.getSuccess());
        assertEquals(shop, result.getData());
    }

    @Test
    void queryById_shouldFailWhenCacheClientReturnsNull() {
        doReturn(null).when(cacheClient).queryWithLogicalExpire(eq(CACHE_SHOP_KEY), eq(1L), eq(Shop.class),
                any(Function.class), eq(CACHE_SHOP_TTL), eq(TimeUnit.MINUTES));

        Result result = shopService.queryById(1L);

        assertFalse(result.getSuccess());
        assertEquals("店铺不存在", result.getErrorMsg());
    }

    @Test
    void create_shouldSaveShopWithDefaultsAndAddGeoIndex() {
        ShopCreateRequest request = new ShopCreateRequest();
        request.setName(" 新增商铺 ");
        request.setTypeId(1L);
        request.setImages("cdn.example.com/shop.jpg");
        request.setArea("西湖");
        request.setAddress("文三路1号");
        request.setX(120.1);
        request.setY(30.2);

        Shop savedShop = new Shop()
                .setId(15L)
                .setName("新增商铺")
                .setTypeId(1L)
                .setImages("https://cdn.example.com/shop.jpg")
                .setArea("西湖")
                .setAddress("文三路1号")
                .setX(120.1)
                .setY(30.2)
                .setSold(0)
                .setComments(0)
                .setScore(50);

        when(shopTypeMapper.selectById(1L)).thenReturn(new ShopType().setId(1L));
        when(stringRedisTemplate.opsForGeo()).thenReturn(geoOperations);
        doReturn(savedShop).when(shopService).getById(15L);
        org.mockito.Mockito.doAnswer(invocation -> {
            Shop shop = invocation.getArgument(0);
            shop.setId(15L);
            return true;
        }).when(shopService).save(any(Shop.class));

        Result result = shopService.create(request);

        assertTrue(result.getSuccess());
        assertEquals(savedShop, result.getData());
        ArgumentCaptor<Shop> shopCaptor = ArgumentCaptor.forClass(Shop.class);
        verify(shopService).save(shopCaptor.capture());
        assertEquals("https://cdn.example.com/shop.jpg", shopCaptor.getValue().getImages());
        verify(geoOperations).add(eq(SHOP_GEO_KEY + 1L), any(Point.class), eq("15"));
    }

    @Test
    void update_shouldFailWhenShopIdIsNull() {
        Result result = shopService.update(new Shop());

        assertFalse(result.getSuccess());
        assertEquals("id不能为空", result.getErrorMsg());
    }

    @Test
    void update_shouldUpdateDatabaseAndDeleteCacheWhenShopIdExists() {
        Shop shop = shop(1L, "updated-shop");
        when(shopMapper.updateById(shop)).thenReturn(1);

        Result result = shopService.update(shop);

        assertTrue(result.getSuccess());
        verify(shopMapper).updateById(shop);
        verify(stringRedisTemplate).delete(RedisConstants.CACHE_SHOP_KEY + 1L);
    }

    @Test
    void queryShopByType_shouldQueryDatabaseWhenCoordinatesAreMissing() {
        Shop shop = shop(1L, "type-shop");
        Page<Shop> page = new Page<>();
        page.setRecords(List.of(shop));
        when(queryChainWrapper.eq(any(), eq(1))).thenReturn(queryChainWrapper);
        when(queryChainWrapper.page(any(Page.class))).thenReturn(page);

        Result result = shopService.queryShopByType(1, 1, null, null);

        assertTrue(result.getSuccess());
        assertEquals(List.of(shop), result.getData());
    }

    @Test
    void queryShopByType_shouldQueryGeoAndReturnDistanceOrderedShopsWhenCoordinatesExist() {
        when(stringRedisTemplate.opsForGeo()).thenReturn(geoOperations);
        GeoResult<RedisGeoCommands.GeoLocation<String>> geoResult = new GeoResult<>(
                new RedisGeoCommands.GeoLocation<>("5", new Point(121.1, 31.2)),
                new Distance(123.4));
        when(geoOperations.search(eq(SHOP_GEO_KEY + 1), any(), any(Distance.class), any()))
                .thenReturn(new GeoResults<>(List.of(geoResult)));

        Shop shop = shop(5L, "geo-shop");
        when(queryChainWrapper.in(any(), ArgumentMatchers.<java.util.Collection<?>>any()))
                .thenReturn(queryChainWrapper);
        when(queryChainWrapper.last("order by field(id,5)")).thenReturn(queryChainWrapper);
        when(queryChainWrapper.list()).thenReturn(List.of(shop));

        Result result = shopService.queryShopByType(1, 1, 121.1, 31.2);

        assertTrue(result.getSuccess());
        List<?> shops = (List<?>) result.getData();
        Shop first = (Shop) shops.get(0);
        assertEquals(5L, first.getId());
        assertEquals(123.4, first.getDistance());
    }

    private Shop shop(Long id, String name) {
        Shop shop = new Shop();
        shop.setId(id);
        shop.setName(name);
        return shop;
    }
}
