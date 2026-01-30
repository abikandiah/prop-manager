package com.akandiah.propmanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
		boolean enabled,
		long cacheExpireMinutes,
		int cacheMaxSize) {

	public RateLimitProperties {
		if (cacheExpireMinutes == 0)
			cacheExpireMinutes = 15;
		if (cacheMaxSize == 0)
			cacheMaxSize = 100_000;
	}
}