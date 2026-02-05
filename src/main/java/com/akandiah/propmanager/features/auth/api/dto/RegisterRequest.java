package com.akandiah.propmanager.features.auth.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;

@Schema(description = "Registration request for when email is not provided in JWT")
public record RegisterRequest(
		@Email @Schema(description = "User's email address", example = "user@example.com") String email) {
}
