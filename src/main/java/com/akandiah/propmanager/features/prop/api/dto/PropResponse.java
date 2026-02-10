package com.akandiah.propmanager.features.prop.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.akandiah.propmanager.features.prop.domain.Prop;
import com.akandiah.propmanager.features.prop.domain.PropertyType;

public record PropResponse(
		UUID id,
		String legalName,
		UUID addressId,
		AddressView address,
		PropertyType propertyType,
		String description,
		String parcelNumber,
		UUID ownerId,
		Integer totalArea,
		Integer yearBuilt,
		Integer version,
		Instant createdAt,
		Instant updatedAt) {

	public static PropResponse from(Prop prop) {
		return new PropResponse(
				prop.getId(),
				prop.getLegalName(),
				prop.getAddress() != null ? prop.getAddress().getId() : null,
				prop.getAddress() != null ? AddressView.from(prop.getAddress()) : null,
				prop.getPropertyType(),
				prop.getDescription(),
				prop.getParcelNumber(),
				prop.getOwnerId(),
				prop.getTotalArea(),
				prop.getYearBuilt(),
				prop.getVersion(),
				prop.getCreatedAt(),
				prop.getUpdatedAt());
	}
}
