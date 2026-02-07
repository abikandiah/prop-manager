package com.akandiah.propmanager.features.address.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.akandiah.propmanager.features.address.domain.Address;

public record AddressResponse(
		UUID id,
		String addressLine1,
		String addressLine2,
		String city,
		String stateProvinceRegion,
		String postalCode,
		String countryCode,
		BigDecimal latitude,
		BigDecimal longitude,
		Instant createdAt) {

	public static AddressResponse from(Address address) {
		return new AddressResponse(
				address.getId(),
				address.getAddressLine1(),
				address.getAddressLine2(),
				address.getCity(),
				address.getStateProvinceRegion(),
				address.getPostalCode(),
				address.getCountryCode(),
				address.getLatitude(),
				address.getLongitude(),
				address.getCreatedAt());
	}
}
