package com.akandiah.propmanager.features.user.api.dto;

import java.util.List;
import java.util.UUID;

import com.akandiah.propmanager.common.permission.AccessEntry;
import com.akandiah.propmanager.features.organization.api.dto.UserOrganizationResponse;

/**
 * Current user info for frontend auth verification and display.
 * Returned by GET /api/me when the user is authenticated via JWT.
 */
public record UserInfoResponse(
		UUID id,
		String name,
		String email,
		boolean termsAccepted,
		List<UserOrganizationResponse> organizations,
		List<UiRole> uiRoles,
		List<AccessEntry> access) {
}
