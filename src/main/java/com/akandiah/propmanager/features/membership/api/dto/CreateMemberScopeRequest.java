package com.akandiah.propmanager.features.membership.api.dto;

import java.util.Map;
import java.util.UUID;

import com.akandiah.propmanager.common.permission.ResourceType;

import jakarta.validation.constraints.NotNull;

public record CreateMemberScopeRequest(
		@NotNull(message = "Scope type is required") ResourceType scopeType,
		@NotNull(message = "Scope ID is required") UUID scopeId,
		/** Optional custom permissions. If absent, the row acts as a pure binding row (empty permissions). */
		Map<String, String> permissions) {
}
