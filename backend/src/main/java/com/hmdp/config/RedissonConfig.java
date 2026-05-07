package com.hmdp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * redisson配置类，用于配置 Redis 客户端。
 * 主要包括连接池、超时时间等。
 */
@Configuration
public class RedissonConfig {
    @Value("${spring.redis.host}")
    private String host;
    @Value("${spring.redis.port}")
    private String port;
    @Value("${spring.redis.password}")
    private String password;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        if (password == null || password.isEmpty()) {
            config.useSingleServer().setAddress("redis://" + host + ":" + port);
        } else {
            config.useSingleServer().setAddress("redis://" + host + ":" + port).setPassword(password);
        }
        return Redisson.create(config);
    }

}
