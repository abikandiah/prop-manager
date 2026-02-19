package com.akandiah.propmanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.invite")
public record InviteProperties(
		int expiryHours,
		int resendCooldownMinutes) {

	public InviteProperties {
		if (expiryHours == 0)
			expiryHours = 72;
		if (resendCooldownMinutes == 0)
			resendCooldownMinutes = 15;
	}
}
