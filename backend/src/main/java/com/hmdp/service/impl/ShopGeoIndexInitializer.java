package com.hmdp.service.impl;

import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.hmdp.utils.RedisConstants.SHOP_GEO_KEY;

@Slf4j
@Component
public class ShopGeoIndexInitializer implements ApplicationRunner {

    private static final double MIN_REDIS_LATITUDE = -85.05112878;
    private static final double MAX_REDIS_LATITUDE = 85.05112878;

    @Resource
    private ShopMapper shopMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void run(ApplicationArguments args) {
        rebuildGeoIndex();
    }

    void rebuildGeoIndex() {
        clearExistingGeoKeys();

        List<Shop> shops = shopMapper.selectList(null);
        Map<Long, List<RedisGeoCommands.GeoLocation<String>>> locationsByType = new HashMap<>();
        int skipped = 0;

        for (Shop shop : shops) {
            if (!hasValidGeoPoint(shop)) {
                skipped++;
                continue;
            }
            locationsByType
                    .computeIfAbsent(shop.getTypeId(), key -> new ArrayList<>())
                    .add(new RedisGeoCommands.GeoLocation<>(
                            shop.getId().toString(),
                            new Point(shop.getX(), shop.getY())));
        }

        int indexed = 0;
        for (Map.Entry<Long, List<RedisGeoCommands.GeoLocation<String>>> entry : locationsByType.entrySet()) {
            List<RedisGeoCommands.GeoLocation<String>> locations = entry.getValue();
            stringRedisTemplate.opsForGeo().add(SHOP_GEO_KEY + entry.getKey(), locations);
            indexed += locations.size();
        }

        log.info("Shop GEO index rebuilt: indexed={}, skipped={}, groups={}", indexed, skipped, locationsByType.size());
    }

    private void clearExistingGeoKeys() {
        Set<String> keys = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions()
                .match(SHOP_GEO_KEY + "*")
                .count(1000)
                .build();

        try (Cursor<String> cursor = stringRedisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        }

        if (!keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    private boolean hasValidGeoPoint(Shop shop) {
        return shop != null
                && shop.getId() != null
                && shop.getTypeId() != null
                && isValidLongitude(shop.getX())
                && isValidLatitude(shop.getY());
    }

    private boolean isValidLongitude(Double value) {
        return value != null && Double.isFinite(value) && value >= -180 && value <= 180;
    }

    private boolean isValidLatitude(Double value) {
        return value != null
                && Double.isFinite(value)
                && value >= MIN_REDIS_LATITUDE
                && value <= MAX_REDIS_LATITUDE;
    }
}
