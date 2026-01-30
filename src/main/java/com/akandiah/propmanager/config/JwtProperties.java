package com.akandiah.propmanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * JWT configuration properties. In production, set these via environment
 * variables
 * or a secrets manager. Never commit secret keys.
 */
@ConfigurationProperties(prefix = "app.jwt")
@Validated
public record JwtProperties(
		@NotBlank String secret,
		@Positive long expirationMs,
		@NotBlank String issuer,
		@NotBlank String headerName,
		@NotBlank String headerPrefix) {
}
