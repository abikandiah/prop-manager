package com.akandiah.propmanager.features.prop.api.dto;

import jakarta.validation.constraints.Size;

public record UpdatePropRequest(
		@Size(max = 255)
		String name,

		@Size(max = 4096)
		String description) {
}
