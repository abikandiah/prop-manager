package com.akandiah.propmanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.notification")
public record NotificationProperties(Email email) {

	public record Email(@DefaultValue("true") boolean enabled) {
	}
}
