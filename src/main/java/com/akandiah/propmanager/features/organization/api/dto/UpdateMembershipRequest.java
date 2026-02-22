package com.akandiah.propmanager.features.organization.api.dto;

import java.util.Map;

import com.akandiah.propmanager.features.organization.domain.Role;

import jakarta.validation.constraints.NotNull;

public record UpdateMembershipRequest(
		@NotNull(message = "Role is required") Role role,
		/** Optional: domain key → action letters. Validated when present. */
		Map<String, String> permissions,
		@NotNull(message = "version is required for optimistic-lock verification") Integer version) {
}
