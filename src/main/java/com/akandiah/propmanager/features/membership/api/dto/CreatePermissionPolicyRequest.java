package com.akandiah.propmanager.features.membership.api.dto;

import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePermissionPolicyRequest(
		/** Client-supplied entity ID. Null is accepted — the server generates a UUID v7. */
		UUID id,

		@NotBlank(message = "Name is required") @Size(max = 255) String name,

		/** Organization ID; null for system-wide policy (requires ADMIN role). */
		UUID orgId,

		/** Flat permissions map: domain key → action letters. Must not be empty. */
		@NotNull(message = "permissions is required") Map<String, String> permissions) {
}
