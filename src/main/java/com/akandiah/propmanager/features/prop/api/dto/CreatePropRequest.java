package com.akandiah.propmanager.features.prop.api.dto;

import java.util.UUID;

import com.akandiah.propmanager.features.prop.domain.PropertyType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePropRequest(
		@NotBlank(message = "Legal name is required")
		@Size(max = 255)
		String legalName,

		@NotNull(message = "Address ID is required")
		UUID addressId,

		@NotNull(message = "Property type is required")
		PropertyType propertyType,

		@Size(max = 64)
		String parcelNumber,

		UUID ownerId,

		Integer totalArea,

		Integer yearBuilt,

		Boolean isActive) {
}
