package com.hmdp.controller;

import com.hmdp.dto.Result;
import com.hmdp.service.IFollowService;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;

/**
 * 用户关注关系接口控制器
 *
 * 负责：
 * 1. 关注 / 取消关注用户
 * 2. 判断当前用户是否已关注目标用户
 * 3. 查询当前用户与目标用户的共同关注
 */
@RestController
@RequestMapping("/follow")
public class FollowController {

    /**
     * 关注业务服务
     * 具体的关注关系维护、Redis Set 处理、共同关注查询等逻辑放在 Service 层
     */
    @Resource
    private IFollowService followService;

    /**
     * 关注或取消关注指定用户
     *
     * @param followUserId 被关注用户的 ID
     * @param isFollow     true 表示关注，false 表示取消关注
     * @return 操作结果
     */
    @PutMapping("/{id}/{isFollow}")
    public Result follow(@PathVariable("id") Long followUserId,
            @PathVariable("isFollow") Boolean isFollow) {
        // 根据 isFollow 判断是新增关注关系，还是删除已有关注关系
        return followService.follow(followUserId, isFollow);
    }

    /**
     * 判断当前登录用户是否关注了指定用户
     *
     * @param followUserId 被查询的用户 ID
     * @return true 表示已关注，false 表示未关注
     */
    @GetMapping("/or/not/{id}")
    public Result isFollow(@PathVariable("id") Long followUserId) {
        // 查询当前用户与目标用户之间是否存在关注关系
        return followService.isFollow(followUserId);
    }

    /**
     * 查询当前登录用户与指定用户的共同关注
     *
     * @param id 目标用户 ID
     * @return 共同关注的用户列表
     */
    @GetMapping("/common/{id}")
    public Result followCommons(@PathVariable("id") Long id) {
        // 通常基于 Redis Set 求交集，得到两个用户共同关注的人
        return followService.followCommons(id);
    }
}