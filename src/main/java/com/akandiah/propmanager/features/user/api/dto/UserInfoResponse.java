package com.akandiah.propmanager.features.user.api.dto;

import java.util.List;
import java.util.UUID;

/**
 * Current user info for frontend auth verification and display.
 * Returned by GET /api/me when the user is authenticated via JWT.
 */
public record UserInfoResponse(
		UUID id,
		String name,
		String email,
		List<String> roles,
		boolean termsAccepted) {
}
