package com.xxx.it.works.wecode.v2.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置
 *
 * <p>提供审计日志异步写入的线程池</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Configuration
public class AsyncConfig {

    /**
     * 审计日志异步写入线程池
     *
     * <p>配置说明：</p>
     * <ul>
     *   <li>corePoolSize=2：核心线程数，常驻 2 个线程处理审计日志</li>
     *   <li>maxPoolSize=5：最大线程数，高并发时扩展到 5 个</li>
     *   <li>queueCapacity=200：队列容量，最多缓冲 200 条待写入日志</li>
     *   <li>CallerRunsPolicy：队列满时由调用线程同步执行，保证不丢日志</li>
     * </ul>
     *
     * @return 线程池执行器
     */
    @Bean("auditLogExecutor")
    public Executor auditLogExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("audit-log-");
        // 队列满时由调用线程同步执行，保证审计日志不丢失
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        log.info("Audit log async executor initialized (core=2, max=5, queue=200)");
        return executor;
    }
}
