package com.profmojo.config;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuration for asynchronous processing in ProfMojo.
 * <p>
 * Provides a dedicated bounded thread pool executor for background email delivery,
 * complete with MDC correlation ID propagation and abort-on-saturation policy.
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {

    public static final String MAIL_EXECUTOR_BEAN = "mailTaskExecutor";

    @Bean(name = MAIL_EXECUTOR_BEAN)
    public Executor mailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("mail-exec-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setTaskDecorator(new CorrelationIdTaskDecorator());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        log.info("Initialized mailTaskExecutor (core=2, max=5, queue=50, prefix='mail-exec-')");
        return executor;
    }

    /**
     * Decorator that captures SLF4J MDC (including correlationId) from the caller thread
     * and sets it on the async worker thread, ensuring MDC is always cleaned up after execution.
     */
    public static class CorrelationIdTaskDecorator implements TaskDecorator {
        @Override
        public Runnable decorate(Runnable runnable) {
            Map<String, String> contextMap = MDC.getCopyOfContextMap();
            return () -> {
                try {
                    if (contextMap != null) {
                        MDC.setContextMap(contextMap);
                    } else {
                        MDC.clear();
                    }
                    runnable.run();
                } finally {
                    MDC.clear();
                }
            };
        }
    }
}
