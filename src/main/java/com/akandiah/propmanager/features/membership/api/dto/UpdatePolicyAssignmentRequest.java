package com.akandiah.propmanager.features.membership.api.dto;

import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record UpdatePolicyAssignmentRequest(
		/** Optional policy ID to link. Set to null to remove policy reference. */
		UUID policyId,

		/** Optional custom permission overrides. Set to null to clear overrides. */
		Map<String, String> overrides,

		@NotNull(message = "version is required for optimistic-lock verification") Integer version) {
}
