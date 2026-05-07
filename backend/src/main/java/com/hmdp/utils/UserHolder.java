package com.hmdp.utils;

import com.hmdp.dto.UserDTO;

/**
 * 当前登录用户上下文工具类。
 *
 * <p>
 * 基于 ThreadLocal 保存当前请求线程内的用户信息，方便业务代码通过
 * {@link #getUser()} 获取当前登录用户，避免层层传参。
 * </p>
 *
 * <p>
 * 使用流程：RefreshTokenInterceptor 根据请求 token 从 Redis Hash 中查询用户信息，
 * 查询成功后调用 {@link #saveUser(UserDTO)} 写入当前线程；请求结束时调用
 * {@link #removeUser()} 清理线程上下文。
 * </p>
 *
 * <p>
 * Redis 负责保存跨请求的登录态，ThreadLocal 负责保存单次请求内的用户上下文。
 * 由于 Web 容器会复用线程，请求结束后必须清理 ThreadLocal，避免用户信息残留或内存泄漏。
 * </p>
 */
public class UserHolder {
    private static final ThreadLocal<UserDTO> tl = new ThreadLocal<>();

    public static void saveUser(UserDTO user) {
        tl.set(user);
    }

    public static UserDTO getUser() {
        return tl.get();
    }

    public static void removeUser() {
        tl.remove();
    }
}