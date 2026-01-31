package com.akandiah.propmanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Audit logging configuration bound to {@code app.audit} in application.yml.
 * Controls what is included in each audit log entry (principal, method, URI, status, duration).
 */
@ConfigurationProperties(prefix = "app.audit")
public record AuditProperties(
		boolean enabled,
		boolean includeQueryString,
		boolean includeClientInfo) {

	public AuditProperties {
		// defaults applied when not set in yaml
	}
}
