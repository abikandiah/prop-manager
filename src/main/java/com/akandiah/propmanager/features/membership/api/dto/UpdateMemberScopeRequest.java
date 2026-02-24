package com.akandiah.propmanager.features.membership.api.dto;

import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record UpdateMemberScopeRequest(
		/** Optional: domain key → action letters. Validated when present. Defaults to empty or template. */
		Map<String, String> permissions,
		/** Optional: copy default_permissions from this template (system or same-org). */
		UUID templateId,
		@NotNull(message = "version is required for optimistic-lock verification") Integer version) {
}
