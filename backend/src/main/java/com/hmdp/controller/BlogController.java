package com.hmdp.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.service.IBlogService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import java.util.List;

/**
 * 博客接口控制器
 *
 * 主要负责：
 * 1. 发布博客
 * 2. 点赞/取消点赞
 * 3. 查询我的笔记
 * 4. 查询热门笔记
 * 5. 查询笔记详情
 * 6. 查询笔记点赞用户
 * 7. 查询指定用户的笔记
 * 8. 查询关注用户的笔记推送流
 */
@RestController
@RequestMapping("/blog")
public class BlogController {

    /**
     * 博客业务服务
     */
    @Resource
    private IBlogService blogService;

    /**
     * 发布探店笔记
     *
     * @param blog 前端提交的博客内容
     * @return 发布结果
     */
    @PostMapping
    public Result saveBlog(@RequestBody Blog blog) {
        // 保存笔记，并在 Service 层处理用户信息、推送到粉丝收件箱等业务逻辑
        return blogService.saveBlog(blog);
    }

    /**
     * 点赞或取消点赞探店笔记
     *
     * @param id 笔记 ID
     * @return 点赞处理结果
     */
    @PutMapping("/like/{id}")
    public Result likeBlog(@PathVariable("id") Long id) {
        // 具体点赞状态判断、点赞数更新、Redis ZSet 记录等逻辑交给 Service 层处理
        return blogService.likeBlog(id);
    }

    /**
     * 分页查询当前登录用户发布的探店笔记
     *
     * @param current 当前页码，默认第 1 页
     * @return 当前用户的笔记列表
     */
    @GetMapping("/of/me")
    public Result queryMyBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        // 从 ThreadLocal 中获取当前登录用户信息
        UserDTO user = UserHolder.getUser();

        // 根据当前登录用户 ID 分页查询笔记
        Page<Blog> page = blogService.query()
                .eq("user_id", user.getId())
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));

        // 只返回当前页的记录列表，不直接返回分页对象
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }

    /**
     * 分页查询热门探店笔记
     *
     * @param current 当前页码，默认第 1 页
     * @return 按热度排序后的笔记列表
     */
    @GetMapping("/hot")
    public Result queryHotBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        // 热门排序、用户信息填充、是否点赞状态判断等逻辑交给 Service 层处理
        return blogService.queryHotBlog(current);
    }

    /**
     * 根据 ID 查询探店笔记详情
     *
     * @param id 笔记 ID
     * @return 笔记详情
     */
    @GetMapping("/{id}")
    public Result queryBlogById(@PathVariable("id") Long id) {
        // 查询笔记详情，并由 Service 层补充作者信息、当前用户是否点赞等数据
        return blogService.queryBlogById(id);
    }

    /**
     * 查询某篇笔记的点赞用户列表
     *
     * @param id 笔记 ID
     * @return 最近点赞该笔记的用户列表
     */
    @GetMapping("/likes/{id}")
    public Result queryBlogLikesById(@PathVariable("id") Long id) {
        // 通常基于 Redis ZSet 查询点赞用户，并保持点赞时间顺序
        return blogService.queryBlogLikesById(id);
    }

    /**
     * 分页查询指定用户发布的探店笔记
     *
     * @param current 当前页码，默认第 1 页
     * @param id      用户 ID
     * @return 指定用户的笔记列表
     */
    @GetMapping("/of/user")
    public Result queryBlogByUserId(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam("id") Long id) {
        // 根据传入的用户 ID 分页查询该用户发布的笔记
        Page<Blog> page = blogService.query()
                .eq("user_id", id)
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));

        // 返回当前页笔记列表
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }

    /**
     * 查询关注用户的探店笔记推送流
     *
     * @param max    本次查询的最大时间戳/游标，用于滚动分页
     * @param offset 偏移量，用于处理相同时间戳下的分页问题
     * @return 关注用户发布的笔记列表和下一次滚动分页参数
     */
    @GetMapping("/of/follow")
    public Result queryBlogOfFollow(@RequestParam("lastId") Long max,
            @RequestParam(value = "offset", defaultValue = "0") Integer offset) {
        // 基于 Redis ZSet 收件箱实现关注 Feed 流滚动分页
        return blogService.queryBlogOfFollow(max, offset);
    }
}