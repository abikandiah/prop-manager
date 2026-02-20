package com.akandiah.propmanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.notification")
public record NotificationProperties(Email email) {

	public record Email(boolean enabled) {

		public Email {
			// defaults to true if not set — matches yaml default
		}
	}
}
