package com.akandiah.propmanager.features.organization.api.dto;

import java.util.Map;
import java.util.UUID;

import com.akandiah.propmanager.common.permission.ResourceType;

import jakarta.validation.constraints.NotNull;

public record CreateMemberScopeRequest(
		@NotNull(message = "Scope type is required") ResourceType scopeType,
		@NotNull(message = "Scope ID is required") UUID scopeId,
		Map<String, String> permissions,
		UUID templateId) {
}
