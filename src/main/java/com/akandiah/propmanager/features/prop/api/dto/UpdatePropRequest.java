package com.akandiah.propmanager.features.prop.api.dto;

import java.util.UUID;

import com.akandiah.propmanager.features.prop.domain.PropertyType;

import jakarta.validation.constraints.Size;

public record UpdatePropRequest(
		@Size(max = 255)
		String legalName,

		UUID addressId,

		PropertyType propertyType,

		@Size(max = 64)
		String parcelNumber,

		UUID ownerId,

		Integer totalArea,

		Integer yearBuilt,

		Boolean isActive) {
}
