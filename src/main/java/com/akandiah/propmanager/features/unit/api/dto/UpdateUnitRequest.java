package com.akandiah.propmanager.features.unit.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.akandiah.propmanager.features.unit.domain.UnitStatus;
import com.akandiah.propmanager.features.unit.domain.UnitType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUnitRequest(
		UUID propertyId,

		@Size(max = 64) String unitNumber,

		UnitStatus status,

		UnitType unitType,

		@Size(max = 2000) String description,

		BigDecimal rentAmount,

		BigDecimal securityDeposit,

		Integer bedrooms,

		Integer bathrooms,

		Integer squareFootage,

		Boolean balcony,

		Boolean laundryInUnit,

		Boolean hardwoodFloors,

		@NotNull(message = "version is required for optimistic-lock verification") Integer version) {
}
