package com.akandiah.propmanager.features.membership.api.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.akandiah.propmanager.features.membership.domain.PermissionPolicy;

public record PermissionPolicyResponse(
		UUID id,
		/** Organization ID; null for system-wide policies. */
		UUID orgId,
		String name,
		Map<String, String> permissions,
		Integer version,
		Instant createdAt,
		Instant updatedAt) {

	public static PermissionPolicyResponse from(PermissionPolicy p) {
		return new PermissionPolicyResponse(
				p.getId(),
				p.getOrg() != null ? p.getOrg().getId() : null,
				p.getName(),
				p.getPermissions(),
				p.getVersion(),
				p.getCreatedAt(),
				p.getUpdatedAt());
	}
}
