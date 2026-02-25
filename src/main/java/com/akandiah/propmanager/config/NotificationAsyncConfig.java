package com.akandiah.propmanager.config;

import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Async executor for outbound email / notification delivery.
 *
 * <p>
 * Email is I/O-bound (SMTP handshake, TLS, network RTT), so a thread pool
 * larger than CPU count is appropriate. Threads are named
 * {@code email-worker-N}
 * to make them easy to identify in logs and thread dumps.
 *
 * <p>
 * Back-pressure: if both the pool and queue are full, the task is dropped with
 * a
 * warning log. The outbox pattern guarantees a PENDING row was committed before
 * the
 * task was enqueued, so the retry scheduler will recover any dropped tasks
 * safely.
 */
@Configuration
@EnableAsync
public class NotificationAsyncConfig {

	private static final Logger log = LoggerFactory.getLogger(NotificationAsyncConfig.class);

	@Bean(name = "notificationExecutor")
	public Executor notificationExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

		// I/O-bound work: 10 concurrent SMTP connections is safe for a medium app.
		executor.setCorePoolSize(10);
		executor.setMaxPoolSize(25);

		// Bounded queue — keeps memory pressure predictable under burst load.
		executor.setQueueCapacity(500);

		// Discard-with-log: the PENDING outbox row is the durable record;
		// the scheduler will recover any dropped tasks on its next cycle.
		executor.setRejectedExecutionHandler((r, exec) -> log
				.warn("Notification task dropped due to full queue — scheduler will recover via PENDING row"));

		executor.setThreadNamePrefix("email-worker-");

		// Wait for in-flight SMTP handshakes to complete before shutdown.
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(60);

		executor.initialize();
		return executor;
	}
}
