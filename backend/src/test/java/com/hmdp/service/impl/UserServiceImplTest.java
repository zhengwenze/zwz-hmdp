package com.hmdp.service.impl;

import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.utils.UserHolder;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_CODE_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_CODE_TTL;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;
import static com.hmdp.utils.RedisConstants.USER_SIGN_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final String PHONE = "13800138000";
    private static final String CODE = "123456";

    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private Environment environment;

    @Mock
    private HttpSession session;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
    }

    @AfterEach
    void tearDown() {
        UserHolder.removeUser();
    }

    @Test
    void sendCode_shouldFailWhenPhoneIsInvalid() {
        Result result = userService.sendCode("bad-phone", session);

        assertFalse(result.getSuccess());
        assertEquals("手机号格式错误", result.getErrorMsg());
        verify(stringRedisTemplate, never()).opsForValue();
    }

    @Test
    void sendCode_shouldWriteCodeToRedisWithTtlWhenPhoneIsValid() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(environment.getActiveProfiles()).thenReturn(new String[] { "dev" });

        Result result = userService.sendCode(PHONE, session);

        assertTrue(result.getSuccess());
        assertNotNull(result.getData());
        assertEquals(6, result.getData().toString().length());
        verify(valueOperations).set(eq(LOGIN_CODE_KEY + PHONE), eq(result.getData().toString()),
                eq(LOGIN_CODE_TTL), eq(TimeUnit.MINUTES));
    }

    @Test
    void login_shouldFailWhenPhoneIsInvalid() {
        LoginFormDTO loginForm = new LoginFormDTO();
        loginForm.setPhone("bad-phone");
        loginForm.setCode(CODE);

        Result result = userService.login(loginForm, session);

        assertFalse(result.getSuccess());
        assertEquals("手机号格式错误", result.getErrorMsg());
        verify(stringRedisTemplate, never()).opsForValue();
    }

    @Test
    void login_shouldFailWhenCodeIsMissingOrWrong() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(LOGIN_CODE_KEY + PHONE)).thenReturn("654321");

        LoginFormDTO loginForm = loginForm(PHONE, CODE);

        Result result = userService.login(loginForm, session);

        assertFalse(result.getSuccess());
        assertEquals("验证码错误", result.getErrorMsg());
        verify(userMapper, never()).selectOne(any());
    }

    @Test
    void login_shouldCreateUserWhenCodeIsCorrectAndUserDoesNotExist() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(valueOperations.get(LOGIN_CODE_KEY + PHONE)).thenReturn(CODE);
        when(userMapper.selectOne(any())).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(10L);
            return 1;
        });

        Result result = userService.login(loginForm(PHONE, CODE), session);

        assertTrue(result.getSuccess());
        assertNotNull(result.getData());
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        assertEquals(PHONE, userCaptor.getValue().getPhone());
        assertTrue(userCaptor.getValue().getNickName().startsWith("user_"));
        verifyLoginTokenWritten(10L);
    }

    @Test
    void login_shouldUseExistingUserWhenCodeIsCorrect() {
        User existingUser = new User();
        existingUser.setId(11L);
        existingUser.setPhone(PHONE);
        existingUser.setNickName("existing-user");
        existingUser.setIcon("icon.png");

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(valueOperations.get(LOGIN_CODE_KEY + PHONE)).thenReturn(CODE);
        when(userMapper.selectOne(any())).thenReturn(existingUser);

        Result result = userService.login(loginForm(PHONE, CODE), session);

        assertTrue(result.getSuccess());
        assertNotNull(result.getData());
        verify(userMapper, never()).insert(any(User.class));
        verifyLoginTokenWritten(11L);
    }

    @Test
    void sign_shouldWriteTodayToRedisBitmap() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        saveCurrentUser(12L);
        LocalDateTime now = LocalDateTime.now();
        String key = USER_SIGN_KEY + now.format(DateTimeFormatter.ofPattern("yyyy:MM:")) + 12L;

        Result result = userService.sign();

        assertTrue(result.getSuccess());
        verify(valueOperations).setBit(key, now.getDayOfMonth() - 1, true);
    }

    @Test
    void signCount_shouldReturnConsecutiveSignedDays() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.bitField(eq(currentSignKey(12L)), any(BitFieldSubCommands.class)))
                .thenReturn(List.of(0b111L));
        saveCurrentUser(12L);

        Result result = userService.signCount();

        assertTrue(result.getSuccess());
        assertEquals(3, result.getData());
    }

    @Test
    void signCount_shouldReturnZeroWhenNoSignDataExists() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.bitField(eq(currentSignKey(12L)), any(BitFieldSubCommands.class))).thenReturn(List.of(0L));
        saveCurrentUser(12L);

        Result result = userService.signCount();

        assertTrue(result.getSuccess());
        assertEquals(0, result.getData());
    }

    private LoginFormDTO loginForm(String phone, String code) {
        LoginFormDTO loginForm = new LoginFormDTO();
        loginForm.setPhone(phone);
        loginForm.setCode(code);
        return loginForm;
    }

    private void saveCurrentUser(Long id) {
        UserDTO user = new UserDTO();
        user.setId(id);
        user.setNickName("current-user");
        UserHolder.saveUser(user);
    }

    private String currentSignKey(Long userId) {
        return USER_SIGN_KEY + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy:MM:")) + userId;
    }

    @SuppressWarnings("unchecked")
    private void verifyLoginTokenWritten(Long expectedUserId) {
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> mapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(hashOperations).putAll(keyCaptor.capture(), mapCaptor.capture());
        String tokenKey = keyCaptor.getValue();
        assertTrue(tokenKey.startsWith(LOGIN_USER_KEY));
        assertTrue(tokenKey.length() > LOGIN_USER_KEY.length());
        assertEquals(expectedUserId.toString(), mapCaptor.getValue().get("id"));

        verify(stringRedisTemplate).expire(tokenKey, LOGIN_USER_TTL, TimeUnit.MINUTES);
    }
}
