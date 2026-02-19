package com.akandiah.propmanager.features.tenant.api.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Partial update for the current user's tenant profile.
 * All fields are optional except {@code version} for optimistic-lock verification.
 * User identity fields (name, email, phone) are managed via PATCH /api/users/me.
 */
public record UpdateTenantRequest(
		String emergencyContactName,
		String emergencyContactPhone,
		Boolean hasPets,
		String petDescription,
		String vehicleInfo,
		String notes,
		@NotNull(message = "version is required for optimistic-lock verification") Integer version) {
}
