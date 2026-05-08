package com.hmdp.controller;

import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.service.IBlogService;
import com.hmdp.utils.UserHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BlogController.class)
@ContextConfiguration(classes = BlogController.class)
class BlogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IBlogService blogService;

    @AfterEach
    void tearDown() {
        UserHolder.removeUser();
    }

    @Test
    void saveBlog_shouldBindRequestBodyAndReturnServiceResult() throws Exception {
        when(blogService.saveBlog(any(Blog.class))).thenReturn(Result.ok(99L));

        mockMvc.perform(post("/blog")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"shopId\":1,\"title\":\"探店标题\",\"content\":\"探店内容\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(99));

        ArgumentCaptor<Blog> captor = ArgumentCaptor.forClass(Blog.class);
        verify(blogService).saveBlog(captor.capture());
        assertEquals(1L, captor.getValue().getShopId());
        assertEquals("探店标题", captor.getValue().getTitle());
    }

    @Test
    void likeBlog_shouldBindBlogIdAndReturnServiceResult() throws Exception {
        when(blogService.likeBlog(5L)).thenReturn(Result.ok());

        mockMvc.perform(put("/blog/like/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(blogService).likeBlog(5L);
    }

    @Test
    void queryMyBlog_shouldUseCurrentUserAndReturnRecords() throws Exception {
        UserDTO user = new UserDTO();
        user.setId(7L);
        UserHolder.saveUser(user);

        QueryChainWrapper<Blog> queryWrapper = org.mockito.Mockito.mock(QueryChainWrapper.class);
        Blog blog = new Blog();
        blog.setId(11L);
        blog.setTitle("我的笔记");
        Page<Blog> page = new Page<>();
        page.setRecords(List.of(blog));

        when(blogService.query()).thenReturn(queryWrapper);
        when(queryWrapper.eq("user_id", 7L)).thenReturn(queryWrapper);
        when(queryWrapper.page(any(Page.class))).thenReturn(page);

        mockMvc.perform(get("/blog/of/me").param("current", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(11))
                .andExpect(jsonPath("$.data[0].title").value("我的笔记"));

        verify(blogService).query();
        verify(queryWrapper).eq("user_id", 7L);
    }

    @Test
    void queryHotBlog_shouldBindCurrentAndReturnServiceResult() throws Exception {
        when(blogService.queryHotBlog(3)).thenReturn(Result.ok("hot"));

        mockMvc.perform(get("/blog/hot").param("current", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("hot"));

        verify(blogService).queryHotBlog(3);
    }

    @Test
    void queryBlogById_shouldBindBlogIdAndReturnServiceResult() throws Exception {
        Blog blog = new Blog();
        blog.setId(5L);
        blog.setTitle("详情");
        when(blogService.queryBlogById(5L)).thenReturn(Result.ok(blog));

        mockMvc.perform(get("/blog/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(5))
                .andExpect(jsonPath("$.data.title").value("详情"));

        verify(blogService).queryBlogById(5L);
    }

    @Test
    void queryBlogLikesById_shouldBindBlogIdAndReturnServiceResult() throws Exception {
        when(blogService.queryBlogLikesById(5L)).thenReturn(Result.ok(List.of("user-a")));

        mockMvc.perform(get("/blog/likes/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0]").value("user-a"));

        verify(blogService).queryBlogLikesById(5L);
    }

    @Test
    void queryBlogByUserId_shouldBindParamsAndReturnRecords() throws Exception {
        QueryChainWrapper<Blog> queryWrapper = org.mockito.Mockito.mock(QueryChainWrapper.class);
        Blog blog = new Blog();
        blog.setId(12L);
        blog.setTitle("用户笔记");
        Page<Blog> page = new Page<>();
        page.setRecords(List.of(blog));

        when(blogService.query()).thenReturn(queryWrapper);
        when(queryWrapper.eq("user_id", 8L)).thenReturn(queryWrapper);
        when(queryWrapper.page(any(Page.class))).thenReturn(page);

        mockMvc.perform(get("/blog/of/user")
                .param("id", "8")
                .param("current", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(12))
                .andExpect(jsonPath("$.data[0].title").value("用户笔记"));

        verify(blogService).query();
        verify(queryWrapper).eq("user_id", 8L);
    }

    @Test
    void queryBlogOfFollow_shouldBindScrollParamsAndReturnServiceResult() throws Exception {
        when(blogService.queryBlogOfFollow(100L, 2)).thenReturn(Result.ok("feed"));

        mockMvc.perform(get("/blog/of/follow")
                .param("lastId", "100")
                .param("offset", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("feed"));

        verify(blogService).queryBlogOfFollow(100L, 2);
    }
}
