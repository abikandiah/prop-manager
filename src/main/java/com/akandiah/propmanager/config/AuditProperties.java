package com.akandiah.propmanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Audit logging configuration bound to {@code app.audit} in application.yml.
 * Controls what is included in each audit log entry (principal, method, URI, status, duration).
 */
@ConfigurationProperties(prefix = "app.audit")
public record AuditProperties(
		@DefaultValue("true") boolean enabled,
		@DefaultValue("true") boolean includeQueryString,
		@DefaultValue("true") boolean includeClientInfo) {
}
