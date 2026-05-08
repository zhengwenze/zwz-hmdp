package com.hmdp.controller;

import com.hmdp.dto.Result;
import com.hmdp.service.IFollowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FollowController.class)
@ContextConfiguration(classes = FollowController.class)
class FollowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IFollowService followService;

    @Test
    void follow_shouldBindPathVariablesAndReturnServiceResult() throws Exception {
        when(followService.follow(2L, true)).thenReturn(Result.ok());

        mockMvc.perform(put("/follow/2/true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(followService).follow(2L, true);
    }

    @Test
    void isFollow_shouldBindPathVariableAndReturnServiceResult() throws Exception {
        when(followService.isFollow(2L)).thenReturn(Result.ok(true));

        mockMvc.perform(get("/follow/or/not/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));

        verify(followService).isFollow(2L);
    }

    @Test
    void followCommons_shouldBindPathVariableAndReturnServiceResult() throws Exception {
        when(followService.followCommons(2L)).thenReturn(Result.ok(List.of("common-user")));

        mockMvc.perform(get("/follow/common/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0]").value("common-user"));

        verify(followService).followCommons(2L);
    }
}
