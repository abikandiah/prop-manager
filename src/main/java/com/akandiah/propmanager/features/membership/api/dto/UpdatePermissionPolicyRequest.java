package com.akandiah.propmanager.features.membership.api.dto;

import java.util.Map;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdatePermissionPolicyRequest(
		@Size(max = 255) String name,

		/** If provided, fully replaces the existing permissions map. */
		Map<String, String> permissions,

		@NotNull(message = "version is required for optimistic-lock verification") Integer version) {
}
