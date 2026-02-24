package com.akandiah.propmanager.config;

import java.util.concurrent.TimeUnit;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
@EnableCaching
@EnableConfigurationProperties(CacheProperties.class)
public class CacheConfig {

	@Bean
	CacheManager cacheManager(CacheProperties properties) {
		CaffeineCacheManager manager = new CaffeineCacheManager("permissions");
		manager.setCaffeine(Caffeine.newBuilder()
				.maximumSize(properties.maxSize())
				.expireAfterWrite(properties.ttlMinutes(), TimeUnit.MINUTES)
				.recordStats());
		return manager;
	}
}
