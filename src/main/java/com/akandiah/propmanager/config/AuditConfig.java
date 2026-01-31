package com.akandiah.propmanager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.akandiah.propmanager.security.AuditLoggingFilter;

/**
 * Registers audit logging filter when {@code app.audit.enabled} is true.
 * Audit entries are written to the {@code AUDIT} logger so they can be
 * routed via Logback (e.g. to a dedicated file or appender).
 */
@Configuration
public class AuditConfig {

	@Bean
	AuditLoggingFilter auditLoggingFilter(AuditProperties properties) {
		return new AuditLoggingFilter(properties);
	}
}
