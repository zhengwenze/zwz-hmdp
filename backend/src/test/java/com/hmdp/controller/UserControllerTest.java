package com.hmdp.controller;

import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.entity.UserInfo;
import com.hmdp.service.IUserInfoService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@ContextConfiguration(classes = UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IUserService userService;

    @MockBean
    private IUserInfoService userInfoService;

    @AfterEach
    void tearDown() {
        UserHolder.removeUser();
    }

    @Test
    void sendCode_shouldBindPhoneAndReturnServiceResult() throws Exception {
        when(userService.sendCode(eq("13800138000"), any(HttpSession.class))).thenReturn(Result.ok("sent"));

        mockMvc.perform(post("/user/code").param("phone", "13800138000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("sent"));

        verify(userService).sendCode(eq("13800138000"), any(HttpSession.class));
    }

    @Test
    void login_shouldBindRequestBodyAndReturnToken() throws Exception {
        when(userService.login(any(LoginFormDTO.class), any(HttpSession.class))).thenReturn(Result.ok("token-1"));

        mockMvc.perform(post("/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"13800138000\",\"code\":\"123456\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("token-1"));

        ArgumentCaptor<LoginFormDTO> captor = ArgumentCaptor.forClass(LoginFormDTO.class);
        verify(userService).login(captor.capture(), any(HttpSession.class));
        assertEquals("13800138000", captor.getValue().getPhone());
        assertEquals("123456", captor.getValue().getCode());
        assertEquals("secret", captor.getValue().getPassword());
    }

    @Test
    void logout_shouldPassAuthorizationHeader() throws Exception {
        when(userService.logout("token-1")).thenReturn(Result.ok());

        mockMvc.perform(post("/user/logout").header("authorization", "token-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(userService).logout("token-1");
    }

    @Test
    void me_shouldReturnCurrentUserFromUserHolder() throws Exception {
        UserDTO user = new UserDTO();
        user.setId(7L);
        user.setNickName("测试用户");
        UserHolder.saveUser(user);

        mockMvc.perform(get("/user/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.nickName").value("测试用户"));
    }

    @Test
    void info_shouldReturnUserInfoWithoutTimestamps() throws Exception {
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(7L);
        userInfo.setCity("上海");
        when(userInfoService.getById(7L)).thenReturn(userInfo);

        mockMvc.perform(get("/user/info/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(7))
                .andExpect(jsonPath("$.data.city").value("上海"));

        verify(userInfoService).getById(7L);
    }

    @Test
    void queryUserById_shouldReturnUserDto() throws Exception {
        User user = new User();
        user.setId(8L);
        user.setNickName("昵称");
        user.setIcon("icon.png");
        when(userService.getById(8L)).thenReturn(user);

        mockMvc.perform(get("/user/8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(8))
                .andExpect(jsonPath("$.data.nickName").value("昵称"))
                .andExpect(jsonPath("$.data.icon").value("icon.png"));

        verify(userService).getById(8L);
    }

    @Test
    void sign_shouldReturnServiceResult() throws Exception {
        when(userService.sign()).thenReturn(Result.ok("signed"));

        mockMvc.perform(post("/user/sign"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("signed"));

        verify(userService).sign();
    }

    @Test
    void signCount_shouldReturnServiceResult() throws Exception {
        when(userService.signCount()).thenReturn(Result.ok(3));

        mockMvc.perform(get("/user/sign/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(3));

        verify(userService).signCount();
    }

    @Test
    void signMonth_shouldReturnServiceResult() throws Exception {
        when(userService.getSignCalendar()).thenReturn(Result.ok("calendar"));

        mockMvc.perform(get("/user/sign/month"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("calendar"));

        verify(userService).getSignCalendar();
    }

    @Test
    void updateNickName_shouldBindParamAndAuthorizationHeader() throws Exception {
        when(userService.updateNickName("新昵称", "token-1")).thenReturn(Result.ok());

        mockMvc.perform(put("/user/nickname")
                .param("nickName", "新昵称")
                .header("authorization", "token-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(userService).updateNickName("新昵称", "token-1");
    }
}
