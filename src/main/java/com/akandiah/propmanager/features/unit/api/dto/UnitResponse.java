package com.akandiah.propmanager.features.unit.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.akandiah.propmanager.features.unit.domain.Unit;
import com.akandiah.propmanager.features.unit.domain.UnitStatus;

public record UnitResponse(
		UUID id,
		UUID propertyId,
		String unitNumber,
		UnitStatus status,
		String description,
		BigDecimal rentAmount,
		BigDecimal securityDeposit,
		Integer bedrooms,
		Integer bathrooms,
		Integer squareFootage,
		Boolean balcony,
		Boolean laundryInUnit,
		Boolean hardwoodFloors,
		Integer version,
		Instant createdAt,
		Instant updatedAt) {

	public static UnitResponse from(Unit unit) {
		return new UnitResponse(
				unit.getId(),
				unit.getProp() != null ? unit.getProp().getId() : null,
				unit.getUnitNumber(),
				unit.getStatus(),
				unit.getDescription(),
				unit.getRentAmount(),
				unit.getSecurityDeposit(),
				unit.getBedrooms(),
				unit.getBathrooms(),
				unit.getSquareFootage(),
				unit.getBalcony(),
				unit.getLaundryInUnit(),
				unit.getHardwoodFloors(),
				unit.getVersion(),
				unit.getCreatedAt(),
				unit.getUpdatedAt());
	}
}
