package com.akandiah.propmanager.features.lease.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.akandiah.propmanager.features.lease.domain.LeaseTenant;
import com.akandiah.propmanager.features.lease.domain.LeaseTenantRole;
import com.akandiah.propmanager.features.lease.domain.LeaseTenantStatus;

public record LeaseTenantResponse(
		UUID id,
		UUID leaseId,
		/** Email address from the originating invite. */
		String email,
		LeaseTenantRole role,
		/** Derived from entity state — not stored in the DB. */
		LeaseTenantStatus status,
		/** Null until the invited user accepts and their tenant profile is linked. */
		UUID tenantId,
		UUID inviteId,
		LocalDate invitedDate,
		LocalDate signedDate,
		Integer version,
		Instant createdAt,
		Instant updatedAt) {

	public static LeaseTenantResponse from(LeaseTenant lt) {
		LeaseTenantStatus status;
		if (lt.getSignedDate() != null) {
			status = LeaseTenantStatus.SIGNED;
		} else if (lt.getTenant() != null) {
			status = LeaseTenantStatus.REGISTERED;
		} else {
			status = LeaseTenantStatus.INVITED;
		}

		return new LeaseTenantResponse(
				lt.getId(),
				lt.getLease().getId(),
				lt.getInvite().getEmail(),
				lt.getRole(),
				status,
				lt.getTenant() != null ? lt.getTenant().getId() : null,
				lt.getInvite().getId(),
				lt.getInvitedDate(),
				lt.getSignedDate(),
				lt.getVersion(),
				lt.getCreatedAt(),
				lt.getUpdatedAt());
	}
}
