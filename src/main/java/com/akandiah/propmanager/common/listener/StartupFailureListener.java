package com.akandiah.propmanager.common.listener;

import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Listener that catches {@link ApplicationFailedEvent} to ensure startup
 * failures
 * are logged with full stack traces, even if the logging system isn't fully
 * initialized
 * or the exception is swallowed by the container.
 */
@Slf4j
@Component
public class StartupFailureListener implements ApplicationListener<ApplicationFailedEvent> {

	@Override
	public void onApplicationEvent(ApplicationFailedEvent event) {
		if (event.getException() != null) {
			String message = "CRITICAL FAILURE: " + event.getException().getMessage();
			// Ensure it hits console regardless of logback state
			System.err.println(message);
			event.getException().printStackTrace();
			// Also log it properly
			log.error(message, event.getException());
		}
	}
}
