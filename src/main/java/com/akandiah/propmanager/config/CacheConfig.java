package com.akandiah.propmanager.config;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Qualifier;
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

	public static final String CACHE_PERMISSIONS = "permissions";

	@Bean
	CacheManager cacheManager(@Qualifier("permissionCacheBuilder") Caffeine<Object, Object> caffeine) {
		CaffeineCacheManager manager = new CaffeineCacheManager();
		manager.setCaffeine(caffeine);
		manager.setCacheNames(List.of(CACHE_PERMISSIONS));
		return manager;
	}

	@Bean("permissionCacheBuilder")
	Caffeine<Object, Object> caffeineConfig(CacheProperties properties) {
		return Caffeine.newBuilder()
				.maximumSize(properties.maxSize())
				.expireAfterWrite(properties.ttlMinutes(), TimeUnit.MINUTES)
				.recordStats();
	}
}
