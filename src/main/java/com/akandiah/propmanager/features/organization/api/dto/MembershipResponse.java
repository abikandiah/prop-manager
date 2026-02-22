package com.akandiah.propmanager.features.organization.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.akandiah.propmanager.features.organization.domain.Membership;
import com.akandiah.propmanager.features.organization.domain.Role;

public record MembershipResponse(
		UUID id,
		UUID userId,
		UUID organizationId,
		Role role,
		Integer version,
		Instant createdAt,
		Instant updatedAt) {

	public static MembershipResponse from(Membership m) {
		return new MembershipResponse(
				m.getId(),
				m.getUser().getId(),
				m.getOrganization().getId(),
				m.getRole(),
				m.getVersion(),
				m.getCreatedAt(),
				m.getUpdatedAt());
	}
}
