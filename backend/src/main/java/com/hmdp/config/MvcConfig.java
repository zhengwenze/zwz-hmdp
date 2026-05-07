package com.hmdp.config;

import com.hmdp.interceptor.LoginInterceptor;
import com.hmdp.interceptor.RefreshTokenInterceptor;
import com.hmdp.utils.SystemConstants;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.annotation.Resource;
import java.nio.file.Paths;

/**
 * mvc配置类，用于配置 Web 应用的 MVC 组件。
 * 主要包括拦截器、资源处理器等。
 * 这个类的组件会被 Spring Boot 自动扫描并加载。
 * 1. 登陆拦截器：用于检查用户是否已登录。
 * 2. Token续命拦截器：用于刷新用户 Token。
 * 3. 资源处理器：用于处理静态资源的请求，如图片、CSS等。
 */
@Configuration
public class MvcConfig implements WebMvcConfigurer {
        @Resource
        private StringRedisTemplate stringRedisTemplate;

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
                // 登陆拦截器
                registry
                                .addInterceptor(new LoginInterceptor())
                                .excludePathPatterns("/user/code", "/user/login", "/blog/hot", "/shop/**",
                                                "/shop-type/**", "/upload/**", "/voucher/**", "/blogs/**",
                                                "/rag/chat", "/rag/status", "/rag/rebuild")
                                .order(1);
                // Token续命拦截器
                registry
                                .addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate))
                                .addPathPatterns("/**")
                                .order(0);
        }

        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
                registry.addResourceHandler("/blogs/**")
                                .addResourceLocations(Paths.get(SystemConstants.IMAGE_UPLOAD_DIR, "blogs").toUri()
                                                .toString());
        }
}
