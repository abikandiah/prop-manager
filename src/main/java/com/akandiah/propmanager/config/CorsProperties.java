package com.akandiah.propmanager.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CORS configuration bound to {@code app.cors} in application.yml.
 * Uses Spring's standard {@link org.springframework.web.cors.CorsConfiguration}.
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
		boolean enabled,
		List<String> allowedOrigins,
		List<String> allowedMethods,
		List<String> allowedHeaders,
		List<String> exposedHeaders,
		Boolean allowCredentials,
		Long maxAge) {

	public CorsProperties {
		if (allowedOrigins == null)
			allowedOrigins = List.of();
		if (allowedMethods == null || allowedMethods.isEmpty())
			allowedMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
		if (allowedHeaders == null || allowedHeaders.isEmpty())
			allowedHeaders = List.of("*");
		if (exposedHeaders == null)
			exposedHeaders = List.of();
		if (allowCredentials == null)
			allowCredentials = true;
		if (maxAge == null)
			maxAge = 3600L;
	}
}
