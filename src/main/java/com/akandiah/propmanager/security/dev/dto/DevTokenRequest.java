package com.akandiah.propmanager.security.dev.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for POST /dev/token. Only accepted when dev profile is active.
 */
public record DevTokenRequest(
		@NotBlank(message = "Username is required")
		String username,

		@NotBlank(message = "Password is required")
		String password) {
}
