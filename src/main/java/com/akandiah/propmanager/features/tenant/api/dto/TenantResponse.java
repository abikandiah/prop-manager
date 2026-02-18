package com.akandiah.propmanager.features.tenant.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.akandiah.propmanager.features.tenant.domain.Tenant;

public record TenantResponse(
		UUID id,
		UUID userId,
		String name,
		String email,
		String phoneNumber,
		String avatarUrl,
		String emergencyContactName,
		String emergencyContactPhone,
		Boolean hasPets,
		String petDescription,
		String vehicleInfo,
		String notes,
		Integer version,
		Instant createdAt,
		Instant updatedAt) {

	public static TenantResponse from(Tenant tenant) {
		return new TenantResponse(
				tenant.getId(),
				tenant.getUser().getId(),
				tenant.getUser().getName(),
				tenant.getUser().getEmail(),
				tenant.getUser().getPhoneNumber(),
				tenant.getUser().getAvatarUrl(),
				tenant.getEmergencyContactName(),
				tenant.getEmergencyContactPhone(),
				tenant.getHasPets(),
				tenant.getPetDescription(),
				tenant.getVehicleInfo(),
				tenant.getNotes(),
				tenant.getVersion(),
				tenant.getCreatedAt(),
				tenant.getUpdatedAt());
	}
}
