package com.akandiah.propmanager.features.lease.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;


import com.akandiah.propmanager.features.lease.domain.LeaseTenant;
import com.akandiah.propmanager.features.lease.domain.LeaseTenantRole;
import com.akandiah.propmanager.features.lease.domain.LeaseTenantStatus;
import com.akandiah.propmanager.features.notification.domain.NotificationDelivery;
import com.akandiah.propmanager.features.notification.domain.NotificationDeliveryStatus;

public record LeaseTenantResponse(
		UUID id,
		UUID leaseId,
		/** Email address from the originating invite. */
		String email,
		LeaseTenantRole role,
		/** Persisted status — set at invite creation (INVITED), acceptance (REGISTERED), and signing (SIGNED). */
		LeaseTenantStatus status,
		/** Null until the invited user accepts and their tenant profile is linked. */
		UUID tenantId,
		UUID inviteId,
		LocalDate invitedDate,
		LocalDate signedDate,
		/** When the tenant accepted the invite. Null until accepted. */
		Instant inviteAcceptedAt,
		/** Lease version at time of signing. Used to detect stale signatures after lease edits. */
		Integer acceptedLeaseVersion,
		/** Timestamp of the last resend, null if never resent. */
		Instant lastResentAt,
		/** Expiry timestamp of the originating invite. */
		Instant expiresAt,
		/** Status of the most recent invite email delivery attempt, null if not yet attempted. */
		NotificationDeliveryStatus latestEmailStatus,
		/** Error detail from the last failed send attempt, null if last send succeeded. */
		String latestEmailError,
		Integer version,
		Instant createdAt,
		Instant updatedAt) {

	/** Build response without delivery information (e.g. immediately after invite creation). */
	public static LeaseTenantResponse from(LeaseTenant lt) {
		return from(lt, null);
	}

	/** Build response with the latest email delivery status for the originating invite. */
	public static LeaseTenantResponse from(LeaseTenant lt, NotificationDelivery latestDelivery) {
		return new LeaseTenantResponse(
				lt.getId(),
				lt.getLease().getId(),
				lt.getInvite().getEmail(),
				lt.getRole(),
				lt.getStatus(),
				lt.getTenant() != null ? lt.getTenant().getId() : null,
				lt.getInvite().getId(),
				lt.getInvitedDate(),
				lt.getSignedDate(),
				lt.getInviteAcceptedAt(),
				lt.getAcceptedLeaseVersion(),
				lt.getInvite().getLastResentAt(),
				lt.getInvite().getExpiresAt(),
				latestDelivery != null ? latestDelivery.getStatus() : null,
				latestDelivery != null ? latestDelivery.getErrorMessage() : null,
				lt.getVersion(),
				lt.getCreatedAt(),
				lt.getUpdatedAt());
	}
}
