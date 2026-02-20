package com.akandiah.propmanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Top-level application properties bound to {@code app} in application.yml.
 * Contains settings that are not specific to any single sub-system.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(String baseUrl) {

	public AppProperties {
		if (baseUrl == null || baseUrl.isBlank())
			baseUrl = "http://localhost:8080";
	}
}
