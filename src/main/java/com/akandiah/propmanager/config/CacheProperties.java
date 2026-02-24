package com.akandiah.propmanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cache.permissions")
public record CacheProperties(
		long maxSize,
		long ttlMinutes) {

	public CacheProperties {
		if (maxSize == 0)
			maxSize = 10_000;
		if (ttlMinutes == 0)
			ttlMinutes = 60;
	}
}
