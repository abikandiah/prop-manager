package com.akandiah.propmanager.features.tenant.api.dto;

import java.util.UUID;

import com.akandiah.propmanager.features.unit.domain.Unit;
import com.akandiah.propmanager.features.unit.domain.UnitType;

public record TenantUnitSummary(
		UUID id,
		String unitNumber,
		UnitType unitType,
		Integer bedrooms,
		Integer bathrooms,
		Integer squareFootage,
		Boolean balcony,
		Boolean laundryInUnit,
		Boolean hardwoodFloors) {

	public static TenantUnitSummary from(Unit unit) {
		return new TenantUnitSummary(
				unit.getId(),
				unit.getUnitNumber(),
				unit.getUnitType(),
				unit.getBedrooms(),
				unit.getBathrooms(),
				unit.getSquareFootage(),
				unit.getBalcony(),
				unit.getLaundryInUnit(),
				unit.getHardwoodFloors());
	}
}
