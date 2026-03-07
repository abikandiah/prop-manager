package com.akandiah.propmanager.features.tenant.api.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.akandiah.propmanager.features.asset.domain.Asset;
import com.akandiah.propmanager.features.asset.domain.AssetCategory;

public record TenantAssetSummary(
		UUID id,
		AssetCategory category,
		String makeModel,
		String serialNumber,
		LocalDate installDate,
		LocalDate warrantyExpiry,
		LocalDate lastServiceDate) {

	public static TenantAssetSummary from(Asset asset) {
		return new TenantAssetSummary(
				asset.getId(),
				asset.getCategory(),
				asset.getMakeModel(),
				asset.getSerialNumber(),
				asset.getInstallDate(),
				asset.getWarrantyExpiry(),
				asset.getLastServiceDate());
	}
}
