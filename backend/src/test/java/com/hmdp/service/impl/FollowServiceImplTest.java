package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class FollowServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Long FOLLOW_USER_ID = 2L;

    @Spy
    private FollowServiceImpl followService;

    @Mock
    private FollowMapper followMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private IUserService userService;

    @Mock
    private LambdaQueryChainWrapper<Follow> queryChainWrapper;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(followService, "baseMapper", followMapper);
        ReflectionTestUtils.setField(followService, "stringRedisTemplate", stringRedisTemplate);
        ReflectionTestUtils.setField(followService, "userService", userService);
        lenient().when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        lenient().doReturn(queryChainWrapper).when(followService).lambdaQuery();
        lenient().when(queryChainWrapper.eq(any(), any())).thenReturn(queryChainWrapper);

        UserDTO user = new UserDTO();
        user.setId(USER_ID);
        user.setNickName("current-user");
        UserHolder.saveUser(user);
    }

    @AfterEach
    void tearDown() {
        UserHolder.removeUser();
    }

    @Test
    void follow_shouldSaveRelationAndCacheWhenNotFollowedYet() {
        when(queryChainWrapper.count()).thenReturn(0L);
        when(followMapper.insert(any(Follow.class))).thenReturn(1);

        Result result = followService.follow(FOLLOW_USER_ID, true);

        assertTrue(result.getSuccess());

        ArgumentCaptor<Follow> captor = ArgumentCaptor.forClass(Follow.class);
        verify(followMapper).insert(captor.capture());
        assertEquals(USER_ID, captor.getValue().getUserId());
        assertEquals(FOLLOW_USER_ID, captor.getValue().getFollowUserId());
        verify(setOperations).add("follows:" + USER_ID, FOLLOW_USER_ID.toString());
    }

    @Test
    void follow_shouldNotInsertAgainAndShouldRefreshCacheWhenAlreadyFollowed() {
        when(queryChainWrapper.count()).thenReturn(1L);

        Result result = followService.follow(FOLLOW_USER_ID, true);

        assertTrue(result.getSuccess());
        verify(followMapper, never()).insert(any(Follow.class));
        verify(setOperations).add("follows:" + USER_ID, FOLLOW_USER_ID.toString());
    }

    @Test
    void follow_shouldTreatDuplicateKeyAsIdempotentSuccessAndRefreshCache() {
        when(queryChainWrapper.count()).thenReturn(0L);
        when(followMapper.insert(any(Follow.class))).thenThrow(new DuplicateKeyException("duplicate follow"));

        Result result = followService.follow(FOLLOW_USER_ID, true);

        assertTrue(result.getSuccess());
        verify(setOperations).add("follows:" + USER_ID, FOLLOW_USER_ID.toString());
    }

    @Test
    void unfollow_shouldRemoveRelationAndEvictCache() {
        when(followMapper.delete(any(Wrapper.class))).thenReturn(1);

        Result result = followService.follow(FOLLOW_USER_ID, false);

        assertTrue(result.getSuccess());
        verify(followMapper).delete(any(Wrapper.class));
        verify(setOperations).remove("follows:" + USER_ID, FOLLOW_USER_ID.toString());
    }

    @Test
    void unfollow_shouldEvictCacheEvenWhenDatabaseDeletesNoRows() {
        when(followMapper.delete(any(Wrapper.class))).thenReturn(0);

        Result result = followService.follow(FOLLOW_USER_ID, false);

        assertTrue(result.getSuccess());
        verify(followMapper).delete(any(Wrapper.class));
        verify(setOperations).remove("follows:" + USER_ID, FOLLOW_USER_ID.toString());
    }

    @Test
    void isFollow_shouldReturnTrueWhenRelationExists() {
        when(queryChainWrapper.count()).thenReturn(1L);

        Result result = followService.isFollow(FOLLOW_USER_ID);

        assertTrue(result.getSuccess());
        assertEquals(true, result.getData());
    }

    @Test
    void isFollow_shouldReturnFalseWhenRelationDoesNotExist() {
        when(queryChainWrapper.count()).thenReturn(0L);

        Result result = followService.isFollow(FOLLOW_USER_ID);

        assertTrue(result.getSuccess());
        assertEquals(false, result.getData());
    }

    @Test
    void followCommons_shouldReturnEmptyListWhenRedisIntersectionIsEmpty() {
        when(setOperations.intersect("follows:" + USER_ID, "follows:" + FOLLOW_USER_ID)).thenReturn(Set.of());

        Result result = followService.followCommons(FOLLOW_USER_ID);

        assertTrue(result.getSuccess());
        assertInstanceOf(List.class, result.getData());
        assertTrue(((List<?>) result.getData()).isEmpty());
        verify(userService, never()).listByIds(any());
    }

    @Test
    void followCommons_shouldQueryUsersAndReturnUserDtosWhenRedisIntersectionExists() {
        when(setOperations.intersect("follows:" + USER_ID, "follows:" + FOLLOW_USER_ID)).thenReturn(Set.of("3", "4"));

        User user3 = new User();
        user3.setId(3L);
        user3.setNickName("user-3");
        user3.setIcon("icon-3.png");
        User user4 = new User();
        user4.setId(4L);
        user4.setNickName("user-4");
        when(userService.listByIds(any())).thenReturn(List.of(user3, user4));

        Result result = followService.followCommons(FOLLOW_USER_ID);

        assertTrue(result.getSuccess());
        List<?> data = (List<?>) result.getData();
        assertEquals(2, data.size());
        UserDTO first = (UserDTO) data.get(0);
        assertEquals(3L, first.getId());
        assertEquals("user-3", first.getNickName());
        assertEquals("icon-3.png", first.getIcon());
        verify(userService).listByIds(any());
    }
}
