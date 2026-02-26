package com.akandiah.propmanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
		@DefaultValue("false") boolean enabled,
		@DefaultValue("15") long cacheExpireMinutes,
		@DefaultValue("100000") int cacheMaxSize) {
}