package com.akandiah.propmanager.features.prop.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.akandiah.propmanager.features.address.api.dto.AddressResponse;
import com.akandiah.propmanager.features.prop.domain.Prop;
import com.akandiah.propmanager.features.prop.domain.PropertyType;

public record PropResponse(
		UUID id,
		String legalName,
		UUID addressId,
		AddressResponse address,
		PropertyType propertyType,
		String parcelNumber,
		UUID ownerId,
		Integer totalArea,
		Integer yearBuilt,
		Boolean isActive,
		Instant createdAt,
		Instant updatedAt) {

	public static PropResponse from(Prop prop) {
		return new PropResponse(
				prop.getId(),
				prop.getLegalName(),
				prop.getAddress() != null ? prop.getAddress().getId() : null,
				prop.getAddress() != null ? AddressResponse.from(prop.getAddress()) : null,
				prop.getPropertyType(),
				prop.getParcelNumber(),
				prop.getOwnerId(),
				prop.getTotalArea(),
				prop.getYearBuilt(),
				prop.getIsActive(),
				prop.getCreatedAt(),
				prop.getUpdatedAt());
	}
}
