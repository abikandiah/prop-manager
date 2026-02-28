package com.akandiah.propmanager.features.membership.api.dto;

import java.util.Map;
import java.util.UUID;

import com.akandiah.propmanager.common.permission.ResourceType;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record CreatePolicyAssignmentRequest(
		/** Client-supplied entity ID. Null is accepted — the server generates a UUID v7. */
		UUID id,

		@NotNull(message = "resourceType is required") ResourceType resourceType,
		@NotNull(message = "resourceId is required") UUID resourceId,

		/** Optional policy to apply. At least one of policyId or overrides must be provided. */
		UUID policyId,

		/** Optional custom permission overrides. Takes precedence over policy permissions when present. */
		Map<String, String> overrides) {

	@AssertTrue(message = "At least one of policyId or overrides must be provided")
	public boolean isAtLeastOneDefined() {
		boolean hasOverrides = overrides != null && !overrides.isEmpty();
		return policyId != null || hasOverrides;
	}
}
