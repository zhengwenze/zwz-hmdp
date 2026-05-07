package com.hmdp.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 声明这是一个 Spring 配置类，Spring 启动时会扫描它，并加载里面定义的 Bean。
// 创建一个 MyBatis Plus 拦截器。
// 给这个拦截器添加分页能力，并指定数据库类型是 MySQL。
// MyBatis Plus 在执行 SQL 时，会使用这个拦截器增强 SQL 功能。
@Configuration
public class MybatisConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
