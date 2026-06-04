package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.ShopCreateRequest;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.SystemConstants;
import org.springframework.data.geo.Point;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.Resource;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    private static final double SHOP_GEO_RADIUS_KM = 10;
    private static final double MIN_LONGITUDE = -180;
    private static final double MAX_LONGITUDE = 180;
    private static final double MIN_REDIS_LATITUDE = -85.05112878;
    private static final double MAX_REDIS_LATITUDE = 85.05112878;

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private CacheClient cacheClient;
    @Resource
    private ShopTypeMapper shopTypeMapper;

    // 1，根据id查询商户信息
    @Override
    public Result queryById(Long id) {
        // 商铺详情缓存统一委托 CacheClient，避免 service 中重复维护缓存策略。
        Shop shop = cacheClient.queryWithLogicalExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL,
                TimeUnit.MINUTES);
        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result create(ShopCreateRequest request) {
        String name = normalize(request.getName());
        String images = normalizeImageList(request.getImages());
        String area = normalize(request.getArea());
        String address = normalize(request.getAddress());
        String openHours = normalize(request.getOpenHours());

        if (StrUtil.isBlank(name)) {
            return Result.fail("商铺名称不能为空");
        }
        if (request.getTypeId() == null) {
            return Result.fail("商铺分类不能为空");
        }
        if (StrUtil.isBlank(images)) {
            return Result.fail("商铺图片必须是 http 或 https 网络链接，多个链接用英文逗号分隔");
        }
        if (StrUtil.isBlank(address)) {
            return Result.fail("商铺地址不能为空");
        }
        if (request.getX() == null || request.getY() == null) {
            return Result.fail("经纬度不能为空");
        }
        if (shopTypeMapper.selectById(request.getTypeId()) == null) {
            return Result.fail("商铺分类不存在");
        }

        Shop shop = new Shop()
                .setName(name)
                .setTypeId(request.getTypeId())
                .setImages(images)
                .setArea(area)
                .setAddress(address)
                .setX(request.getX())
                .setY(request.getY())
                .setAvgPrice(request.getAvgPrice())
                .setSold(0)
                .setComments(0)
                .setScore(request.getScore() == null ? 50 : request.getScore())
                .setOpenHours(openHours);

        boolean saved = save(shop);
        if (!saved) {
            return Result.fail("创建商铺失败");
        }

        addShopToGeoIndex(shop);

        return Result.ok(getById(shop.getId()));
    }

    // 2，更新店铺信息
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("id不能为空");
        }
        Shop oldShop = getById(id);
        // 更新数据库
        boolean updated = updateById(shop);
        if (!updated) {
            return Result.fail("店铺不存在");
        }
        Shop newShop = getById(id);
        syncGeoIndexAfterUpdate(oldShop, newShop);
        // 删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());
        return Result.ok();
    }

    // 3，按类型查询商店
    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        Result validationResult = validateQueryByTypeParams(typeId, current, x, y);
        if (validationResult != null) {
            return validationResult;
        }

        if (!hasCoordinate(x, y)) {
            return queryShopByTypeFromDb(typeId, current);
        }
        return queryShopByTypeWithGeo(typeId, current, x, y);
    }

    private Result validateQueryByTypeParams(Integer typeId, Integer current, Double x, Double y) {
        if (typeId == null) {
            return Result.fail("商铺类型不能为空");
        }
        if (current == null || current < 1) {
            return Result.fail("页码必须大于0");
        }
        if ((x == null) != (y == null)) {
            return Result.fail("经纬度必须同时传入");
        }
        if (hasCoordinate(x, y) && (!isValidLongitude(x) || !isValidLatitude(y))) {
            return Result.fail("经纬度范围不合法");
        }
        return null;
    }

    private Result queryShopByTypeFromDb(Integer typeId, Integer current) {
        Page<Shop> page = lambdaQuery()
                .eq(Shop::getTypeId, typeId)
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        return Result.ok(page.getRecords());
    }

    private Result queryShopByTypeWithGeo(Integer typeId, Integer current, Double x, Double y) {
        int from = pageOffset(current);
        int end = geoLimit(current);

        String key = SHOP_GEO_KEY + typeId;
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo()
                .search(key, GeoReference.fromCoordinate(x, y), new Distance(SHOP_GEO_RADIUS_KM, Metrics.KILOMETERS),
                        RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end));
        if (results == null) {
            return Result.ok(Collections.emptyList());
        }

        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> content = results.getContent();
        if (content.size() <= from) {
            return Result.ok(Collections.emptyList());
        }

        List<Long> ids = new ArrayList<>(SystemConstants.MAX_PAGE_SIZE);
        Map<Long, Distance> distanceMap = new HashMap<>();
        content.stream().skip(from).limit(SystemConstants.MAX_PAGE_SIZE).forEach(result -> {
            String shopId = result.getContent().getName();
            Long id = Long.valueOf(shopId);
            ids.add(id);
            distanceMap.put(id, result.getDistance());
        });
        if (ids.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }

        List<Shop> shopList = listByIdsKeepGeoOrder(ids);
        for (Shop shop : shopList) {
            Distance distance = distanceMap.get(shop.getId());
            if (distance != null) {
                shop.setDistance(distance.getValue() * 1000);
            }
        }
        return Result.ok(shopList);
    }

    private int pageOffset(Integer current) {
        return (current - 1) * SystemConstants.MAX_PAGE_SIZE;
    }

    private int geoLimit(Integer current) {
        return current * SystemConstants.MAX_PAGE_SIZE;
    }

    private List<Shop> listByIdsKeepGeoOrder(List<Long> ids) {
        String join = StrUtil.join(",", ids);
        return lambdaQuery()
                .in(Shop::getId, ids)
                .last("order by field(id," + join + ")")
                .list();
    }

    private boolean hasCoordinate(Double x, Double y) {
        return x != null && y != null;
    }

    private boolean isValidLongitude(Double value) {
        return value != null && Double.isFinite(value) && value >= MIN_LONGITUDE && value <= MAX_LONGITUDE;
    }

    private boolean isValidLatitude(Double value) {
        return value != null
                && Double.isFinite(value)
                && value >= MIN_REDIS_LATITUDE
                && value <= MAX_REDIS_LATITUDE;
    }

    private void syncGeoIndexAfterUpdate(Shop oldShop, Shop newShop) {
        if (newShop == null) {
            if (oldShop != null) {
                removeShopFromGeoIndex(oldShop);
            }
            return;
        }
        if (oldShop == null || isGeoIndexChanged(oldShop, newShop)) {
            if (oldShop != null) {
                removeShopFromGeoIndex(oldShop);
            }
            addShopToGeoIndex(newShop);
        }
    }

    private boolean isGeoIndexChanged(Shop oldShop, Shop newShop) {
        return !Objects.equals(oldShop.getTypeId(), newShop.getTypeId())
                || !Objects.equals(oldShop.getX(), newShop.getX())
                || !Objects.equals(oldShop.getY(), newShop.getY());
    }

    private void addShopToGeoIndex(Shop shop) {
        if (!hasGeoIndexData(shop)) {
            return;
        }
        stringRedisTemplate.opsForGeo().add(
                SHOP_GEO_KEY + shop.getTypeId(),
                new Point(shop.getX(), shop.getY()),
                shop.getId().toString());
    }

    private void removeShopFromGeoIndex(Shop shop) {
        if (shop == null || shop.getId() == null || shop.getTypeId() == null) {
            return;
        }
        stringRedisTemplate.opsForGeo().remove(SHOP_GEO_KEY + shop.getTypeId(), shop.getId().toString());
    }

    private boolean hasGeoIndexData(Shop shop) {
        return shop != null
                && shop.getId() != null
                && shop.getTypeId() != null
                && shop.getX() != null
                && shop.getY() != null;
    }

    private String normalize(String value) {
        return StrUtil.isBlank(value) ? null : value.trim();
    }

    private String normalizeImageList(String images) {
        if (StrUtil.isBlank(images)) {
            return null;
        }
        List<String> urls = new ArrayList<>();
        for (String image : images.split(",")) {
            String url = normalize(image);
            if (StrUtil.isBlank(url)) {
                return null;
            }
            if (url.startsWith("//")) {
                url = "https:" + url;
            } else if (!(url.startsWith("http://") || url.startsWith("https://"))) {
                if (url.startsWith("/") || !url.contains(".")) {
                    return null;
                }
                url = "https://" + url;
            }
            urls.add(url);
        }
        return StrUtil.join(",", urls);
    }
}
