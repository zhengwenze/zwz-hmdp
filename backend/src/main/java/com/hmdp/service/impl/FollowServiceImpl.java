package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {
    private static final String FOLLOW_KEY_PREFIX = "follows:";

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IUserService userService;

    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        //获取登陆用户
        Long id = UserHolder.getUser().getId();
        //判断是关注还是取关
        if (isFollow) {
            Long count = lambdaQuery()
                    .eq(Follow::getUserId, id)
                    .eq(Follow::getFollowUserId, followUserId)
                    .count();
            if (count > 0) {
                cacheFollow(id, followUserId);
                return Result.ok();
            }
            try {
                //关注 新增数据
                Follow follow = new Follow();
                follow.setFollowUserId(followUserId);
                follow.setUserId(id);
                if (save(follow)) {
                    cacheFollow(id, followUserId);
                }
            } catch (DuplicateKeyException e) {
                // 并发重复关注时，数据库唯一约束会拒绝第二次插入；这里按幂等成功处理。
                cacheFollow(id, followUserId);
            }
        } else {
            //取关 删除
            remove(new LambdaQueryWrapper<Follow>()
                    .eq(Follow::getUserId, id)
                    .eq(Follow::getFollowUserId, followUserId)
            );
            evictFollowCache(id, followUserId);
        }
        return Result.ok();
    }

    @Override
    public Result isFollow(Long followUserId) {
        //获取登陆用户
        Long id = UserHolder.getUser().getId();
        //查询是否关注
        Long count = lambdaQuery()
                .eq(Follow::getUserId, id)
                .eq(Follow::getFollowUserId, followUserId)
                .count();
        return Result.ok(count > 0);
    }

    @Override
    public Result followCommons(Long id) {
        //获取登陆用户
        Long userId = UserHolder.getUser().getId();
        String key = FOLLOW_KEY_PREFIX + userId;
        //求交集
        String key2 = FOLLOW_KEY_PREFIX + id;
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key, key2);
        if (intersect == null || intersect.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        //解析出id
        List<Long> ids = intersect.stream().map(Long::valueOf).collect(Collectors.toList());
        //查询用户
        List<User> users = userService.listByIds(ids);
        List<UserDTO> collect = users.stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(collect);
    }

    private void cacheFollow(Long userId, Long followUserId) {
        try {
            stringRedisTemplate.opsForSet().add(FOLLOW_KEY_PREFIX + userId, followUserId.toString());
        } catch (Exception e) {
            log.warn("Failed to cache follow relation: userId={}, followUserId={}", userId, followUserId, e);
        }
    }

    private void evictFollowCache(Long userId, Long followUserId) {
        try {
            stringRedisTemplate.opsForSet().remove(FOLLOW_KEY_PREFIX + userId, followUserId.toString());
        } catch (Exception e) {
            log.warn("Failed to evict follow relation cache: userId={}, followUserId={}", userId, followUserId, e);
        }
    }
}
