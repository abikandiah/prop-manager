package com.akandiah.propmanager.features.organization.api.dto;

import java.util.Map;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateOrganizationRequest(
		@Size(max = 255) String name,
		@Size(max = 64) String taxId,
		Map<String, Object> settings,
		@NotNull(message = "version is required for optimistic-lock verification") Integer version) {
}
