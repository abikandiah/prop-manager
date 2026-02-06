package com.akandiah.propmanager.features.auth.api.dto;

import java.util.List;

/**
 * Current user info for frontend auth verification and display.
 * Returned by GET /api/me when the user is authenticated via JWT.
 */
public record UserInfoResponse(
		String sub,
	String name,
	String email,
	List<String> roles) {
}
