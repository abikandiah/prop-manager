package com.akandiah.propmanager.features.asset.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.akandiah.propmanager.features.asset.domain.Asset;
import com.akandiah.propmanager.features.asset.domain.AssetCategory;

public record AssetResponse(
		UUID id,
		UUID propertyId,
		UUID unitId,
		AssetCategory category,
		String makeModel,
		String serialNumber,
		LocalDate installDate,
		LocalDate warrantyExpiry,
		LocalDate lastServiceDate,
		Instant createdAt,
		Instant updatedAt) {

	public static AssetResponse from(Asset asset) {
		return new AssetResponse(
				asset.getId(),
				asset.getProp() != null ? asset.getProp().getId() : null,
				asset.getUnit() != null ? asset.getUnit().getId() : null,
				asset.getCategory(),
				asset.getMakeModel(),
				asset.getSerialNumber(),
				asset.getInstallDate(),
				asset.getWarrantyExpiry(),
				asset.getLastServiceDate(),
				asset.getCreatedAt(),
				asset.getUpdatedAt());
	}
}
