package com.akandiah.propmanager.features.prop.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePropRequest(
		@NotBlank(message = "Name is required")
		@Size(max = 255)
		String name,

		@Size(max = 4096)
		String description) {
}
