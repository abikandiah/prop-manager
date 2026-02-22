package com.akandiah.propmanager.features.permission.api.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.akandiah.propmanager.features.permission.domain.PermissionTemplate;

public record PermissionTemplateResponse(
		UUID id,
		/** Organization ID; null for system-wide template. */
		UUID orgId,
		String name,
		Map<String, String> defaultPermissions,
		Integer version,
		Instant createdAt,
		Instant updatedAt) {

	public static PermissionTemplateResponse from(PermissionTemplate t) {
		return new PermissionTemplateResponse(
				t.getId(),
				t.getOrg() != null ? t.getOrg().getId() : null,
				t.getName(),
				t.getDefaultPermissions(),
				t.getVersion(),
				t.getCreatedAt(),
				t.getUpdatedAt());
	}
}
