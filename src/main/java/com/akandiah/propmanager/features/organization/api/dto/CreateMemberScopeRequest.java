package com.akandiah.propmanager.features.organization.api.dto;

import java.util.Map;
import java.util.UUID;

import com.akandiah.propmanager.features.organization.domain.ScopeType;

import jakarta.validation.constraints.NotNull;

public record CreateMemberScopeRequest(
		@NotNull(message = "Scope type is required") ScopeType scopeType,
		@NotNull(message = "Scope ID is required") UUID scopeId,
		/** Optional: domain key → action letters. Validated when present. Defaults to empty or template. */
		Map<String, String> permissions,
		/** Optional: copy default_permissions from this template (system or same-org). */
		UUID templateId) {
}
