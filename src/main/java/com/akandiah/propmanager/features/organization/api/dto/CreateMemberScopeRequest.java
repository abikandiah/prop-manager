package com.akandiah.propmanager.features.organization.api.dto;

import java.util.UUID;

import com.akandiah.propmanager.features.organization.domain.ScopeType;

import jakarta.validation.constraints.NotNull;

public record CreateMemberScopeRequest(
		@NotNull(message = "Scope type is required") ScopeType scopeType,
		@NotNull(message = "Scope ID is required") UUID scopeId) {
}
