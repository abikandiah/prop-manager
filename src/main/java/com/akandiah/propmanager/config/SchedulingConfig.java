package com.akandiah.propmanager.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration to enable scheduled tasks.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
	// Scheduling enabled - tasks can now use @Scheduled annotation
}
