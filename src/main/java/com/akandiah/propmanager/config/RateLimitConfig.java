package com.akandiah.propmanager.config;

import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;

@Configuration
public class RateLimitConfig {

	@Bean
	Cache<String, RateLimiter> rateLimiterCache(RateLimitProperties props, RateLimiterRegistry registry) {
		return Caffeine.newBuilder()
				.expireAfterAccess(props.cacheExpireMinutes(), TimeUnit.MINUTES)
				.maximumSize(props.cacheMaxSize())
				.evictionListener((key, value, cause) -> {
					if (key != null)
						registry.remove("api-" + key);
				})
				.build();
	}
}
