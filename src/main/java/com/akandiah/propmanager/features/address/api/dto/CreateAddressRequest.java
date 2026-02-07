package com.akandiah.propmanager.features.address.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAddressRequest(
		@NotBlank(message = "Address line 1 is required")
		@Size(max = 255)
		String addressLine1,

		@Size(max = 255)
		String addressLine2,

		@NotBlank(message = "City is required")
		@Size(max = 100)
		String city,

		@NotBlank(message = "State/Province/Region is required")
		@Size(max = 100)
		String stateProvinceRegion,

		@NotBlank(message = "Postal code is required")
		@Size(max = 20)
		String postalCode,

		@NotBlank(message = "Country code is required")
		@Size(min = 2, max = 2)
		String countryCode,

		BigDecimal latitude,

		BigDecimal longitude) {
}
