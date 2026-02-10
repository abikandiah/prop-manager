package com.akandiah.propmanager.features.prop.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.akandiah.propmanager.features.prop.domain.Address;

public record AddressView(
		UUID id,
		String addressLine1,
		String addressLine2,
		String city,
		String stateProvinceRegion,
		String postalCode,
		String countryCode,
		BigDecimal latitude,
		BigDecimal longitude,
		Instant createdAt,
		Instant updatedAt) {

	public static AddressView from(Address address) {
		return new AddressView(
				address.getId(),
				address.getAddressLine1(),
				address.getAddressLine2(),
				address.getCity(),
				address.getStateProvinceRegion(),
				address.getPostalCode(),
				address.getCountryCode(),
				address.getLatitude(),
				address.getLongitude(),
				address.getCreatedAt(),
				address.getUpdatedAt());
	}
}
