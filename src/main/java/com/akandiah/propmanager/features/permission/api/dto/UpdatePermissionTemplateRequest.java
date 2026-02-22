package com.akandiah.propmanager.features.permission.api.dto;

import java.util.Map;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdatePermissionTemplateRequest(
		@Size(max = 255) String name,

		/** Domain key → action letters. If provided, validated by PermissionStringValidator. */
		Map<String, String> defaultPermissions,

		/**
		 * Required for optimistic-lock verification. Must match the current version on the server.
		 */
		@NotNull(message = "version is required for optimistic-lock verification") Integer version) {
}
