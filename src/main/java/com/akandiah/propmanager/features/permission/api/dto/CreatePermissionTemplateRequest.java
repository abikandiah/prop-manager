package com.akandiah.propmanager.features.permission.api.dto;

import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePermissionTemplateRequest(
		@NotBlank(message = "Name is required") @Size(max = 255) String name,

		/** Organization ID; null for system-wide template. */
		UUID orgId,

		/** Domain key → action letters (e.g. "l" → "cru", "m" → "r"). Validated by PermissionStringValidator. */
		@NotNull(message = "defaultPermissions is required") Map<String, String> defaultPermissions) {
}
