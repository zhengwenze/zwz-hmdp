package com.hmdp.service.impl;

import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.hmdp.utils.RedisConstants.SHOP_GEO_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShopGeoIndexInitializerTest {

    private final ShopGeoIndexInitializer initializer = new ShopGeoIndexInitializer();

    @Mock
    private ShopMapper shopMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private GeoOperations<String, String> geoOperations;

    @Mock
    private Cursor<String> cursor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(initializer, "shopMapper", shopMapper);
        ReflectionTestUtils.setField(initializer, "stringRedisTemplate", stringRedisTemplate);
    }

    @Test
    void rebuildGeoIndex_shouldClearOldKeysAndBatchAddValidShopsByType() {
        when(stringRedisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(true, true, false);
        when(cursor.next()).thenReturn(SHOP_GEO_KEY + 1, SHOP_GEO_KEY + 2);
        when(shopMapper.selectList(null)).thenReturn(List.of(
                shop(1L, 1L, 120.1, 30.1),
                shop(2L, 1L, 120.2, 30.2),
                shop(3L, 2L, 120.3, 30.3),
                shop(4L, 2L, null, 30.4),
                shop(5L, 2L, 120.5, 90.0)));
        when(stringRedisTemplate.opsForGeo()).thenReturn(geoOperations);

        initializer.rebuildGeoIndex();

        verify(stringRedisTemplate).delete(Set.of(SHOP_GEO_KEY + 1, SHOP_GEO_KEY + 2));
        ArgumentCaptorHelper firstGroup = new ArgumentCaptorHelper();
        ArgumentCaptorHelper secondGroup = new ArgumentCaptorHelper();
        verify(geoOperations).add(eq(SHOP_GEO_KEY + 1), firstGroup.capture());
        verify(geoOperations).add(eq(SHOP_GEO_KEY + 2), secondGroup.capture());
        assertEquals(List.of("1", "2"), firstGroup.names());
        assertEquals(List.of("3"), secondGroup.names());
    }

    private Shop shop(Long id, Long typeId, Double x, Double y) {
        return new Shop()
                .setId(id)
                .setTypeId(typeId)
                .setX(x)
                .setY(y);
    }

    private static class ArgumentCaptorHelper {
        private Collection<RedisGeoCommands.GeoLocation<String>> value;

        Collection<RedisGeoCommands.GeoLocation<String>> capture() {
            return ArgumentMatchers.argThat(argument -> {
                value = argument;
                return true;
            });
        }

        List<String> names() {
            return value.stream()
                    .map(location -> location.getName())
                    .toList();
        }
    }
}
