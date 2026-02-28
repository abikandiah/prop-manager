package com.akandiah.propmanager.features.membership.api.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.akandiah.propmanager.common.permission.ResourceType;
import com.akandiah.propmanager.features.membership.domain.PolicyAssignment;

public record PolicyAssignmentResponse(
		UUID id,
		UUID membershipId,
		ResourceType resourceType,
		UUID resourceId,
		UUID policyId,
		Map<String, String> overrides,
		Integer version,
		Instant createdAt,
		Instant updatedAt) {

	public static PolicyAssignmentResponse from(PolicyAssignment a) {
		return from(a, a.getMembership().getId());
	}

	public static PolicyAssignmentResponse from(PolicyAssignment a, UUID membershipId) {
		return new PolicyAssignmentResponse(
				a.getId(),
				membershipId,
				a.getResourceType(),
				a.getResourceId(),
				a.getPolicy() != null ? a.getPolicy().getId() : null,
				a.getOverrides(),
				a.getVersion(),
				a.getCreatedAt(),
				a.getUpdatedAt());
	}
}
