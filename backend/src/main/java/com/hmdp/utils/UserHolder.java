package com.hmdp.utils;

import com.hmdp.dto.UserDTO;

/**
 * 基于 ThreadLocal 的用户上下文持有者
 *
 * <p>
 * 配合 RefreshTokenInterceptor 使用：
 * <ol>
 * <li>请求进入时，拦截器从 Redis Hash 中查询用户信息</li>
 * <li>调用 {@link #saveUser(UserDTO)} 将用户存入当前线程</li>
 * <li>业务代码通过 {@link #getUser()} 获取当前登录用户，无需层层传参</li>
 * <li>请求结束前，拦截器调用 {@link #removeUser()} 清理线程，防止内存泄漏</li>
 * </ol>
 *
 * <p>
 * 注意：每个线程独立存储，因此不同请求（不同线程）互不干扰。
 * 但在线程池环境下，务必在 finally 块中调用 {@link #removeUser()}。
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
