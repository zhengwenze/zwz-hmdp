package com.hmdp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Configuration
public class AsyncExecutorConfig {

    public interface CacheRebuildTask extends Runnable {
        String getCacheKey();

        void onRejected();
    }

    @Bean("seckillOrderExecutor")
    public ThreadPoolTaskExecutor seckillOrderExecutor(AsyncExecutorProperties properties) {
        return buildExecutor("seckillOrderExecutor", properties.getSeckillOrder(),
                new ThreadPoolExecutor.AbortPolicy());
    }

    @Bean("cacheRebuildExecutor")
    public ThreadPoolTaskExecutor cacheRebuildExecutor(AsyncExecutorProperties properties) {
        return buildExecutor("cacheRebuildExecutor", properties.getCacheRebuild(),
                cacheRebuildRejectHandler("cacheRebuildExecutor"));
    }

    @Bean("ragRebuildExecutor")
    public ThreadPoolTaskExecutor ragRebuildExecutor(AsyncExecutorProperties properties) {
        return buildExecutor("ragRebuildExecutor", properties.getRagRebuild(), new ThreadPoolExecutor.AbortPolicy());
    }

    private ThreadPoolTaskExecutor buildExecutor(String beanName, AsyncExecutorProperties.ExecutorProperties properties,
            RejectedExecutionHandler rejectedExecutionHandler) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getCoreSize());
        executor.setMaxPoolSize(properties.getMaxSize());
        executor.setQueueCapacity(properties.getQueueCapacity());
        executor.setKeepAliveSeconds(properties.getKeepAliveSeconds());
        executor.setThreadNamePrefix(properties.getThreadNamePrefix());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(properties.getAwaitTerminationSeconds());
        executor.setRejectedExecutionHandler(rejectedExecutionHandler);
        executor.initialize();
        log.info(
                "Initialized executor [{}]: core={}, max={}, queueCapacity={}, keepAliveSeconds={}, threadNamePrefix={}, awaitTerminationSeconds={}",
                beanName,
                properties.getCoreSize(),
                properties.getMaxSize(),
                properties.getQueueCapacity(),
                properties.getKeepAliveSeconds(),
                properties.getThreadNamePrefix(),
                properties.getAwaitTerminationSeconds());
        return executor;
    }

    private RejectedExecutionHandler cacheRebuildRejectHandler(String executorName) {
        return (runnable, executor) -> {
            String cacheKey = "unknown";
            if (runnable instanceof CacheRebuildTask) {
                CacheRebuildTask task = (CacheRebuildTask) runnable;
                cacheKey = task.getCacheKey();
                task.onRejected();
            }
            log.warn("Cache rebuild task rejected: executor={}, cacheKey={}, activeCount={}, queueSize={}",
                    executorName,
                    cacheKey,
                    executor.getActiveCount(),
                    executor.getQueue().size());
        };
    }
}
