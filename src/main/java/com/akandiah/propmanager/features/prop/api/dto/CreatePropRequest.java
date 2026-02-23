package com.akandiah.propmanager.features.prop.api.dto;

import java.math.BigDecimal;

import com.akandiah.propmanager.features.prop.domain.PropertyType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePropRequest(
		@NotBlank(message = "Legal name is required") @Size(max = 255) String legalName,

		@NotNull(message = "Address is required") @Valid AddressInput address,

		@NotNull(message = "Property type is required") PropertyType propertyType,

		@Size(max = 2000) String description,

		@Size(max = 64) String parcelNumber,

		@NotNull(message = "Organization ID is required") java.util.UUID organizationId,

		java.util.UUID ownerId,

		Integer totalArea,

		Integer yearBuilt) {

	public record AddressInput(
			@NotBlank(message = "Address line 1 is required") @Size(max = 255) String addressLine1,

			@Size(max = 255) String addressLine2,

			@NotBlank(message = "City is required") @Size(max = 100) String city,

			@NotBlank(message = "State/Province/Region is required") @Size(max = 100) String stateProvinceRegion,

			@NotBlank(message = "Postal code is required") @Size(max = 20) String postalCode,

			@NotBlank(message = "Country code is required") @Size(min = 2, max = 2) String countryCode,

			BigDecimal latitude,

			BigDecimal longitude) {
	}
}
