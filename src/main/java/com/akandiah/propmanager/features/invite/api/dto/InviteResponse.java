package com.akandiah.propmanager.features.invite.api.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.akandiah.propmanager.features.invite.domain.Invite;
import com.akandiah.propmanager.features.invite.domain.InviteStatus;
import com.akandiah.propmanager.features.invite.domain.TargetType;

/**
 * Response containing invitation details.
 */
public record InviteResponse(UUID id, String email, TargetType targetType, UUID targetId,
		Map<String, Object> attributes,
		UUID invitedById, String invitedByName, InviteStatus status,
		Instant createdAt, Instant sentAt, Instant lastResentAt, Instant expiresAt,
		Instant acceptedAt, UUID claimedUserId, boolean isValid, boolean isExpired) {

	/**
	 * Create response from Invite entity.
	 */
	public static InviteResponse from(Invite invite) {
		return new InviteResponse(invite.getId(), invite.getEmail(), invite.getTargetType(), invite.getTargetId(),
				invite.getAttributes(), invite.getInvitedBy().getId(), invite.getInvitedBy().getName(),
				invite.getStatus(),
				invite.getCreatedAt(), invite.getSentAt(),
				invite.getLastResentAt(), invite.getExpiresAt(),
				invite.getAcceptedAt(), invite.getClaimedUser() != null ? invite.getClaimedUser().getId() : null,
				invite.isValid(), invite.isExpired());
	}
}
