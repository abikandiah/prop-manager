package com.akandiah.propmanager.features.unit.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.akandiah.propmanager.features.unit.domain.UnitStatus;

import jakarta.validation.constraints.Size;

public record UpdateUnitRequest(
		UUID propertyId,

		@Size(max = 64)
		String unitNumber,

		UnitStatus status,

		BigDecimal rentAmount,

		BigDecimal securityDeposit,

		Integer bedrooms,

		Integer bathrooms,

		Integer squareFootage,

		Boolean balcony,

		Boolean laundryInUnit,

		Boolean hardwoodFloors) {
}
