package com.akandiah.propmanager.features.organization.api.dto;

import java.util.Map;
import java.util.UUID;

import com.akandiah.propmanager.features.organization.domain.Role;

import jakarta.validation.constraints.NotNull;

public record CreateMembershipRequest(
		@NotNull(message = "User ID is required") UUID userId,
		@NotNull(message = "Role is required") Role role,
		/** Optional: domain key → action letters (e.g. "l" → "cru"). Validated with PermissionStringValidator. */
		Map<String, String> permissions,
		/** Optional: copy default_permissions from this template (system or same-org). */
		UUID templateId) {
}
