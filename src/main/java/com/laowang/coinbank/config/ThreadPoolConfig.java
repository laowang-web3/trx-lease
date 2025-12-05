package com.laowang.coinbank.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ThreadPoolConfig {

    /**
     * 自定义线程池：监控器线程池
     */
    @Bean(name = "monitorExecutor") // 命名Bean，方便后续指定使用
    public Executor monitorExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 1. 核心线程数（根据CPU核心数或业务需求配置，如 CPU核心数 * 2）
        executor.setCorePoolSize(4);
        // 2. 最大线程数（核心线程+非核心线程的总上限）
        executor.setMaxPoolSize(20);
        // 3. 任务队列容量（核心线程满后，任务先存到队列）
        executor.setQueueCapacity(100);
        // 4. 非核心线程空闲存活时间（单位：秒）
        executor.setKeepAliveSeconds(60);
        // 5. 线程名称前缀（方便日志排查，如 "business-thread-1"）
        executor.setThreadNamePrefix("monitor-");
        // 6. 拒绝策略（队列、线程都满时的处理方式）
        // 常用策略：CallerRunsPolicy（由提交任务的主线程自己执行，避免任务丢失）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 初始化线程池（必须调用，否则线程池不会生效）
        executor.initialize();
        return executor;
    }
}
