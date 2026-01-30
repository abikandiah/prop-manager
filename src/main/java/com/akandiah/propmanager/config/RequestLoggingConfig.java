package com.akandiah.propmanager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

/**
 * Request logging using Spring's built-in {@link CommonsRequestLoggingFilter}.
 * Set logging level to DEBUG to see request details:
 * 
 * <pre>
 * logging.level.org.springframework.web.filter.CommonsRequestLoggingFilter = DEBUG
 * </pre>
 * 
 * Access logs (method, URI, status, duration) are handled by Tomcat; see
 * {@code server.tomcat.accesslog.*} in application.yaml.
 */
@Configuration
public class RequestLoggingConfig {

	@Bean
	CommonsRequestLoggingFilter commonsRequestLoggingFilter() {
		CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();

		// Core Info
		filter.setIncludeQueryString(true);
		filter.setIncludeClientInfo(true);

		// Security (Keep false for production safety)
		filter.setIncludeHeaders(false);

		// Request Body (Useful for debugging POST/PUT)
		// filter.setIncludePayload(true);
		// filter.setMaxPayloadLength(1000);

		// Custom Formatting
		filter.setBeforeMessagePrefix(">>> REQUEST START: [");
		filter.setAfterMessagePrefix("<<< REQUEST FINISH: [");
		filter.setAfterMessageSuffix("]");

		return filter;
	}
}
