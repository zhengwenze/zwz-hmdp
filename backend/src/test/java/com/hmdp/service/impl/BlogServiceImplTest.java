package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.dto.ScrollResult;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Follow;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 博客服务单元测试
 *
 * @author 郑文泽
 * @since 2026-05-07
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BlogServiceImplTest {

    @Autowired
    private IBlogService blogService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private Long testUserId;
    private Long testBlogId;

    @BeforeEach
    void setUp() {
        // 准备测试数据 - 使用 ID 为 1 的用户（确保数据库中存在）
        testUserId = 1L;
        UserDTO testUser = new UserDTO();
        testUser.setId(testUserId);
        testUser.setNickName("测试用户");
        UserHolder.saveUser(testUser);

        // 清理 Redis 中的测试数据
        cleanTestData();
    }

    @AfterEach
    void tearDown() {
        // 清理测试数据
        cleanTestData();
        UserHolder.removeUser();
    }

    /**
     * 清理测试数据
     */
    private void cleanTestData() {
        if (testBlogId != null) {
            stringRedisTemplate.delete(RedisConstants.BLOG_LIKED_KEY + testBlogId);
        }
    }

    @Test
    @DisplayName("查询热门博客 - 返回点赞数排序的博客列表")
    void testQueryHotBlog() {
        // 执行查询
        Result result = blogService.queryHotBlog(1);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertNotNull(result.getData());

        // 验证返回的是列表
        assertTrue(result.getData() instanceof List);
        List<?> blogs = (List<?>) result.getData();

        // 验证博客对象字段
        if (!blogs.isEmpty()) {
            Object firstBlog = blogs.get(0);
            assertTrue(firstBlog instanceof Blog);
            Blog blog = (Blog) firstBlog;
            assertNotNull(blog.getId());
            assertNotNull(blog.getTitle());
        }
    }

    @Test
    @DisplayName("通过 ID 查询博客 - 博客存在")
    void testQueryBlogById_Exists() {
        // 准备数据：先查询一个存在的博客
        Result hotBlogResult = blogService.queryHotBlog(1);
        List<?> blogs = (List<?>) hotBlogResult.getData();

        if (blogs.isEmpty()) {
            // 如果没有博客，跳过测试
            return;
        }

        testBlogId = ((Blog) blogs.get(0)).getId();

        // 执行查询
        Result result = blogService.queryBlogById(testBlogId);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertNotNull(result.getData());

        Blog blog = (Blog) result.getData();
        assertEquals(testBlogId, blog.getId());
        assertNotNull(blog.getTitle());
        assertNotNull(blog.getUserId());
    }

    @Test
    @DisplayName("通过 ID 查询博客 - 博客不存在")
    void testQueryBlogById_NotExists() {
        // 执行查询（使用一个不存在的 ID）
        Result result = blogService.queryBlogById(999999999L);

        // 验证结果
        assertNotNull(result);
        assertFalse(result.getSuccess());
        assertEquals("博客不存在", result.getErrorMsg());
    }

    @Test
    @DisplayName("点赞博客 - 首次点赞")
    void testLikeBlog_FirstLike() {
        // 准备数据：先查询一个存在的博客
        Result hotBlogResult = blogService.queryHotBlog(1);
        List<?> blogs = (List<?>) hotBlogResult.getData();

        if (blogs.isEmpty()) {
            return;
        }

        testBlogId = ((Blog) blogs.get(0)).getId();
        String likeKey = RedisConstants.BLOG_LIKED_KEY + testBlogId;

        // 确保 Redis 中没有该用户的点赞记录
        stringRedisTemplate.opsForZSet().remove(likeKey, testUserId.toString());

        // 获取点赞前的数量
        Blog blogBefore = blogService.getById(testBlogId);
        int likedCountBefore = blogBefore != null ? blogBefore.getLiked() : 0;

        // 执行点赞
        Result result = blogService.likeBlog(testBlogId);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.getSuccess());

        // 验证 Redis 中有点赞记录
        Double score = stringRedisTemplate.opsForZSet().score(likeKey, testUserId.toString());
        assertNotNull(score);

        // 验证数据库点赞数 +1
        Blog blogAfter = blogService.getById(testBlogId);
        assertNotNull(blogAfter);
        assertEquals(likedCountBefore + 1, blogAfter.getLiked());
    }

    @Test
    @DisplayName("点赞博客 - 取消点赞")
    void testLikeBlog_CancelLike() {
        // 准备数据：先查询一个存在的博客
        Result hotBlogResult = blogService.queryHotBlog(1);
        List<?> blogs = (List<?>) hotBlogResult.getData();

        if (blogs.isEmpty()) {
            return;
        }

        testBlogId = ((Blog) blogs.get(0)).getId();
        String likeKey = RedisConstants.BLOG_LIKED_KEY + testBlogId;

        // 先点赞
        stringRedisTemplate.opsForZSet().add(likeKey, testUserId.toString(), System.currentTimeMillis());
        blogService.update().setSql("liked=liked+1").eq("id", testBlogId).update();

        // 获取点赞前的数量
        Blog blogBefore = blogService.getById(testBlogId);
        int likedCountBefore = blogBefore != null ? blogBefore.getLiked() : 0;

        // 执行取消点赞
        Result result = blogService.likeBlog(testBlogId);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.getSuccess());

        // 验证 Redis 中点赞记录已删除
        Double score = stringRedisTemplate.opsForZSet().score(likeKey, testUserId.toString());
        assertNull(score);

        // 验证数据库点赞数 -1
        Blog blogAfter = blogService.getById(testBlogId);
        assertNotNull(blogAfter);
        assertEquals(likedCountBefore - 1, blogAfter.getLiked());
    }

    @Test
    @DisplayName("查询博客点赞排行榜 - 有点赞用户")
    void testQueryBlogLikesById_WithLikes() {
        // 准备数据：先查询一个存在的博客
        Result hotBlogResult = blogService.queryHotBlog(1);
        List<?> blogs = (List<?>) hotBlogResult.getData();

        if (blogs.isEmpty()) {
            return;
        }

        testBlogId = ((Blog) blogs.get(0)).getId();
        String likeKey = RedisConstants.BLOG_LIKED_KEY + testBlogId;

        // 添加测试点赞数据
        stringRedisTemplate.opsForZSet().add(likeKey, "1", System.currentTimeMillis());
        stringRedisTemplate.opsForZSet().add(likeKey, "2", System.currentTimeMillis() + 1);
        stringRedisTemplate.opsForZSet().add(likeKey, "3", System.currentTimeMillis() + 2);

        // 执行查询
        Result result = blogService.queryBlogLikesById(testBlogId);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertNotNull(result.getData());

        List<?> likes = (List<?>) result.getData();
        assertTrue(likes instanceof List);

        // 验证返回了点赞用户（最多 5 个）
        assertTrue(likes.size() <= 5);
    }

    @Test
    @DisplayName("查询博客点赞排行榜 - 无点赞")
    void testQueryBlogLikesById_NoLikes() {
        // 准备数据：先查询一个存在的博客
        Result hotBlogResult = blogService.queryHotBlog(1);
        List<?> blogs = (List<?>) hotBlogResult.getData();

        if (blogs.isEmpty()) {
            return;
        }

        testBlogId = ((Blog) blogs.get(0)).getId();
        String likeKey = RedisConstants.BLOG_LIKED_KEY + testBlogId;

        // 确保 Redis 中没有点赞记录
        stringRedisTemplate.delete(likeKey);

        // 执行查询
        Result result = blogService.queryBlogLikesById(testBlogId);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertNotNull(result.getData());

        List<?> likes = (List<?>) result.getData();
        assertTrue(likes instanceof List);
        assertTrue(likes.isEmpty());
    }

    @Test
    @DisplayName("保存博客 - 成功")
    @Transactional
    @Rollback(true)
    void testSaveBlog_Success() {
        // 准备博客数据
        Blog blog = new Blog();
        blog.setTitle("测试博客标题");
        blog.setContent("测试博客内容");
        blog.setShopId(1L);
        blog.setImages("test-image.jpg");

        // 执行保存
        Result result = blogService.saveBlog(blog);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertNotNull(result.getData());

        // 验证返回的是博客 ID
        assertTrue(result.getData() instanceof Long);
        testBlogId = (Long) result.getData();

        // 验证数据库中存在
        Blog savedBlog = blogService.getById(testBlogId);
        assertNotNull(savedBlog);
        assertEquals("测试博客标题", savedBlog.getTitle());
        assertEquals(testUserId, savedBlog.getUserId());
    }

    @Test
    @DisplayName("查询关注 Feed 流 - 滚动加载")
    void testQueryBlogOfFollow() {
        // 执行查询（max=0 表示从最新开始，offset=0）
        Result result = blogService.queryBlogOfFollow(System.currentTimeMillis(), 0);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.getSuccess());

        // 验证返回类型
        Object data = result.getData();
        if (data != null) {
            assertTrue(data instanceof ScrollResult);
            ScrollResult scrollResult = (ScrollResult) data;
            assertNotNull(scrollResult.getList());
            assertNotNull(scrollResult.getOffset());
            assertNotNull(scrollResult.getMinTime());
        }
    }

    @Test
    @DisplayName("查询热门博客 - 分页参数验证")
    void testQueryHotBlog_Pagination() {
        // 查询第 1 页
        Result page1 = blogService.queryHotBlog(1);
        assertNotNull(page1);
        assertTrue(page1.getSuccess());

        // 查询第 2 页
        Result page2 = blogService.queryHotBlog(2);
        assertNotNull(page2);
        assertTrue(page2.getSuccess());

        // 验证两页数据不同（如果有足够数据）
        List<?> blogs1 = (List<?>) page1.getData();
        List<?> blogs2 = (List<?>) page2.getData();

        // 如果第 2 页有数据，应该与第 1 页不同
        if (!blogs2.isEmpty()) {
            Blog firstBlogPage1 = (Blog) blogs1.get(0);
            Blog firstBlogPage2 = (Blog) blogs2.get(0);
            assertNotEquals(firstBlogPage1.getId(), firstBlogPage2.getId());
        }
    }

    @Test
    @DisplayName("点赞博客 - 重复点赞处理")
    void testLikeBlog_DuplicateLike() {
        // 准备数据
        Result hotBlogResult = blogService.queryHotBlog(1);
        List<?> blogs = (List<?>) hotBlogResult.getData();

        if (blogs.isEmpty()) {
            return;
        }

        testBlogId = ((Blog) blogs.get(0)).getId();
        String likeKey = RedisConstants.BLOG_LIKED_KEY + testBlogId;

        // 第一次点赞
        Result result1 = blogService.likeBlog(testBlogId);
        assertTrue(result1.getSuccess());

        // 验证已点赞
        Double score = stringRedisTemplate.opsForZSet().score(likeKey, testUserId.toString());
        assertNotNull(score);

        // 第二次点赞（应该取消点赞）
        Result result2 = blogService.likeBlog(testBlogId);
        assertTrue(result2.getSuccess());

        // 验证已取消
        score = stringRedisTemplate.opsForZSet().score(likeKey, testUserId.toString());
        assertNull(score);
    }
}
