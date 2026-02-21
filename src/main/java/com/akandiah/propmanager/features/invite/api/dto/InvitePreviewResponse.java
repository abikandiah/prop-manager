package com.akandiah.propmanager.features.invite.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.akandiah.propmanager.features.invite.domain.InviteStatus;

/**
 * Public-safe invite preview returned by the unauthenticated token-lookup endpoint.
 * Email is masked; no internal IDs or sensitive fields are exposed.
 */
public record InvitePreviewResponse(
		String maskedEmail,
		InviteStatus status,
		boolean isValid,
		boolean isExpired,
		Instant expiresAt,
		String invitedByName,
		PropertyPreview property,
		UnitPreview unit,
		LeasePreview lease) {

	public record PropertyPreview(
			String legalName,
			String addressLine1,
			String addressLine2,
			String city,
			String stateProvinceRegion,
			String postalCode) {
	}

	public record UnitPreview(
			String unitNumber,
			String unitType) {
	}

	public record LeasePreview(
			LocalDate startDate,
			LocalDate endDate,
			BigDecimal rentAmount) {
	}
}
