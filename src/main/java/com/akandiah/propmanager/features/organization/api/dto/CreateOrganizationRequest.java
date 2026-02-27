package com.akandiah.propmanager.features.organization.api.dto;

import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRequest(
		/** Client-supplied entity ID. Null is accepted — the server generates a UUID v7. */
		UUID id,

		@NotBlank(message = "Name is required") @Size(max = 255) String name,
		@Size(max = 64) String taxId,
		Map<String, Object> settings) {
}
