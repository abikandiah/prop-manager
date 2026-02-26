package com.akandiah.propmanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.cache.permissions")
public record CacheProperties(
		@DefaultValue("10000") long maxSize,
		@DefaultValue("60") long ttlMinutes) {
}
