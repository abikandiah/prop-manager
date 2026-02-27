package com.akandiah.propmanager.features.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Result of POST /api/logout.
 */
public record LogoutResponse(
		@Schema(description = "Optional URL to redirect the user to for complete session termination (e.g. OIDC logout).", example = "https://auth.example.com/application/o/app/end-session/") String logoutUrl) {
}
