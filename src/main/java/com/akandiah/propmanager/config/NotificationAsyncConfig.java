package com.akandiah.propmanager.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Async executor for outbound email / notification delivery.
 *
 * <p>Email is I/O-bound (SMTP handshake, TLS, network RTT), so a thread pool
 * larger than CPU count is appropriate. Threads are named {@code email-worker-N}
 * to make them easy to identify in logs and thread dumps.
 *
 * <p>Back-pressure: if both the pool and queue are full, {@link ThreadPoolExecutor.CallerRunsPolicy}
 * causes the calling thread (the async event dispatcher) to send the email directly,
 * which naturally slows the producer without dropping work.
 */
@Configuration
@EnableAsync
public class NotificationAsyncConfig {

    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // I/O-bound work: 10 concurrent SMTP connections is safe for a medium app.
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(25);

        // Bounded queue — keeps memory pressure predictable under burst load.
        // Note: the retry scheduler acts as a durable backstop for any overflow.
        executor.setQueueCapacity(500);

        // CallerRunsPolicy: back-pressure instead of silent drops.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.setThreadNamePrefix("email-worker-");

        // Wait for in-flight SMTP handshakes to complete before shutdown.
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();
        return executor;
    }
}
