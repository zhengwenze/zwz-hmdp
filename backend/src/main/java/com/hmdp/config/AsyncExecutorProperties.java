package com.hmdp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

// 声明这是一个配置类，用于配置异步任务的执行器属性。
// 这个类的属性会被 Spring Boot 自动注入到其他组件中。
@Data
@ConfigurationProperties(prefix = "hmdp.async")
public class AsyncExecutorProperties {

    private ExecutorProperties seckillOrder = new ExecutorProperties();
    private ExecutorProperties cacheRebuild = new ExecutorProperties();
    private ExecutorProperties ragRebuild = new ExecutorProperties();
    private ExecutorProperties ragChatStream = new ExecutorProperties();

    @Data
    public static class ExecutorProperties {
        private int coreSize;
        private int maxSize;
        private int queueCapacity;
        private int keepAliveSeconds;
        private String threadNamePrefix;
        private int awaitTerminationSeconds;
    }
}
