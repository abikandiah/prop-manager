package com.akandiah.propmanager.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * CORS configuration bound to {@code app.cors} in application.yml.
 * Uses Spring's standard {@link org.springframework.web.cors.CorsConfiguration}.
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
		@DefaultValue("false") boolean enabled,
		List<String> allowedOrigins,
		@DefaultValue("GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD") List<String> allowedMethods,
		@DefaultValue("*") List<String> allowedHeaders,
		List<String> exposedHeaders,
		@DefaultValue("true") boolean allowCredentials,
		@DefaultValue("3600") long maxAge) {

	public CorsProperties {
		if (allowedOrigins == null)
			allowedOrigins = List.of();
		if (exposedHeaders == null)
			exposedHeaders = List.of();
	}
}
