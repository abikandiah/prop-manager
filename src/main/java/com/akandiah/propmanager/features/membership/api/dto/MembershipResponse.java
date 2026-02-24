package com.akandiah.propmanager.features.membership.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.akandiah.propmanager.features.invite.domain.InviteStatus;
import com.akandiah.propmanager.features.membership.domain.Membership;

public record MembershipResponse(
		UUID id,
		UUID userId,
		String userName,
		String userEmail,
		UUID organizationId,
		UUID inviteId,
		String inviteEmail,
		InviteStatus inviteStatus,
		Integer version,
		Instant createdAt,
		Instant updatedAt) {

	public static MembershipResponse from(Membership m) {
		return new MembershipResponse(
				m.getId(),
				m.getUser() != null ? m.getUser().getId() : null,
				m.getUser() != null ? m.getUser().getName() : null,
				m.getUser() != null ? m.getUser().getEmail() : null,
				m.getOrganization().getId(),
				m.getInvite() != null ? m.getInvite().getId() : null,
				m.getInvite() != null ? m.getInvite().getEmail() : null,
				m.getInvite() != null ? m.getInvite().getStatus() : null,
				m.getVersion(),
				m.getCreatedAt(),
				m.getUpdatedAt());
	}
}
