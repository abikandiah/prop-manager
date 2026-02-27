package com.akandiah.propmanager.features.asset.api.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.akandiah.propmanager.features.asset.domain.AssetCategory;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAssetRequest(
		UUID id,

		UUID propertyId,

		UUID unitId,

		@NotNull(message = "Category is required")
		AssetCategory category,

		@Size(max = 255)
		String makeModel,

		@Size(max = 128)
		String serialNumber,

		LocalDate installDate,

		LocalDate warrantyExpiry,

		LocalDate lastServiceDate) {
}
