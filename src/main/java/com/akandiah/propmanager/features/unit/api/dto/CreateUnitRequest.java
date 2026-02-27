package com.akandiah.propmanager.features.unit.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.akandiah.propmanager.features.unit.domain.UnitStatus;
import com.akandiah.propmanager.features.unit.domain.UnitType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUnitRequest(
		UUID id,

		@NotNull(message = "Property ID is required") UUID propertyId,

		@NotBlank(message = "Unit number is required") @Size(max = 64) String unitNumber,

		@NotNull(message = "Status is required") UnitStatus status,

		UnitType unitType,

		@Size(max = 2000) String description,

		BigDecimal rentAmount,

		BigDecimal securityDeposit,

		Integer bedrooms,

		Integer bathrooms,

		Integer squareFootage,

		Boolean balcony,

		Boolean laundryInUnit,

		Boolean hardwoodFloors) {
}
