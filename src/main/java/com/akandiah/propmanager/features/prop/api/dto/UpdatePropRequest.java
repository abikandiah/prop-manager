package com.akandiah.propmanager.features.prop.api.dto;

import java.util.UUID;

import com.akandiah.propmanager.features.prop.api.dto.CreatePropRequest.AddressInput;
import com.akandiah.propmanager.features.prop.domain.PropertyType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdatePropRequest(
		@Size(max = 255) String legalName,

		@Valid AddressInput address,

		PropertyType propertyType,

		@Size(max = 2000) String description,

		@Size(max = 64) String parcelNumber,

		UUID organizationId,

		UUID ownerId,

		Integer totalArea,

		Integer yearBuilt,

		@NotNull(message = "version is required for optimistic-lock verification") Integer version) {
}
