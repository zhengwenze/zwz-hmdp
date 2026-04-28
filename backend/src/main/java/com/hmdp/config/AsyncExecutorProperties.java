package com.hmdp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "hmdp.async")
public class AsyncExecutorProperties {

    private ExecutorProperties seckillOrder = new ExecutorProperties();
    private ExecutorProperties cacheRebuild = new ExecutorProperties();
    private ExecutorProperties ragRebuild = new ExecutorProperties();

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
