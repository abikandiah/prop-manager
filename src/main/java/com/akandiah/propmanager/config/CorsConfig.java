package com.akandiah.propmanager.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS configuration using Spring's standard {@link CorsConfigurationSource}.
 * All settings are driven by {@code app.cors} in application.yml.
 */
@Configuration
public class CorsConfig {

	@Bean
	CorsConfigurationSource corsConfigurationSource(CorsProperties props) {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(props.enabled() ? props.allowedOrigins() : List.of());
		config.setAllowedMethods(props.allowedMethods());
		config.setAllowedHeaders(props.allowedHeaders());
		if (!props.exposedHeaders().isEmpty())
			config.setExposedHeaders(props.exposedHeaders());
		config.setAllowCredentials(props.allowCredentials());
		config.setMaxAge(props.maxAge());

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}
