package com.akandiah.propmanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.invite")
public record InviteProperties(
		@DefaultValue("72") int expiryHours,
		@DefaultValue("15") int resendCooldownMinutes,
		@DefaultValue("3") int maxEmailRetries,
		@DefaultValue("15") int emailRetryIntervalMinutes) {
}
