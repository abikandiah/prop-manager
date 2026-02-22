package com.akandiah.propmanager.features.organization.api.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRequest(
		@NotBlank(message = "Name is required") @Size(max = 255) String name,
		@Size(max = 64) String taxId,
		Map<String, Object> settings) {
}
