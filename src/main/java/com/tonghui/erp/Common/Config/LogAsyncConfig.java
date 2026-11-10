package com.tonghui.erp.Common.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 日志异步写入配置
 * <p>
 * 登录日志与操作日志统一使用独立的日志线程池，避免日志落库阻塞业务请求。
 * 队列满时由调用线程执行，保证审计日志不被静默丢弃。
 * </p>
 */
@Configuration
@EnableAsync
public class LogAsyncConfig {

    // region 线程池定义
    // ===================================
    // 线程池定义
    // ===================================

    /**
     * 日志写入线程池
     *
     * @return 日志专用线程池
     */
    @Bean("logExecutor")
    public ThreadPoolTaskExecutor logExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(2000);
        executor.setThreadNamePrefix("log-writer-");
        // 队列满时由调用线程执行，宁可让请求稍慢，也不丢审计日志
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }

    // endregion
}
