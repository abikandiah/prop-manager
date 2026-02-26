package com.akandiah.propmanager.features.membership.api.dto;

import java.util.Map;

import jakarta.validation.constraints.NotNull;

public record UpdateMemberScopeRequest(
		/** Optional custom permissions. If absent or empty, scope acts as a pure binding row. */
		Map<String, String> permissions,
		@NotNull(message = "version is required for optimistic-lock verification") Integer version) {
}
