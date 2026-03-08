package com.akandiah.propmanager.features.user.api.dto;

import jakarta.validation.constraints.Size;

/**
 * Request body for PATCH /api/me — all fields optional.
 */
public record PatchMeRequest(
		Boolean termsAccepted,
		@Size(max = 255) String name,
		@Size(max = 50) String phoneNumber) {
}
