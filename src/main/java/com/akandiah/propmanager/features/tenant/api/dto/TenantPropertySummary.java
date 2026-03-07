package com.akandiah.propmanager.features.tenant.api.dto;

import java.util.UUID;

import com.akandiah.propmanager.features.prop.api.dto.AddressView;
import com.akandiah.propmanager.features.prop.domain.Prop;
import com.akandiah.propmanager.features.prop.domain.PropertyType;

public record TenantPropertySummary(
		UUID id,
		String legalName,
		AddressView address,
		PropertyType propertyType,
		String description,
		Integer yearBuilt,
		String managementPhone,
		String managementEmail) {

	public static TenantPropertySummary from(Prop prop, String managementPhone, String managementEmail) {
		return new TenantPropertySummary(
				prop.getId(),
				prop.getLegalName(),
				prop.getAddress() != null ? AddressView.from(prop.getAddress()) : null,
				prop.getPropertyType(),
				prop.getDescription(),
				prop.getYearBuilt(),
				managementPhone,
				managementEmail);
	}
}
