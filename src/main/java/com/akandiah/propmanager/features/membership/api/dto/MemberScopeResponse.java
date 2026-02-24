package com.akandiah.propmanager.features.membership.api.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.akandiah.propmanager.common.permission.ResourceType;
import com.akandiah.propmanager.features.membership.domain.MemberScope;

public record MemberScopeResponse(
		UUID id,
		UUID membershipId,
		ResourceType scopeType,
		UUID scopeId,
		Map<String, String> permissions,
		Integer version,
		Instant createdAt,
		Instant updatedAt) {

	public static MemberScopeResponse from(MemberScope s) {
		return from(s, s.getMembership().getId());
	}

	public static MemberScopeResponse from(MemberScope s, UUID membershipId) {
		return new MemberScopeResponse(
				s.getId(),
				membershipId,
				s.getScopeType(),
				s.getScopeId(),
				s.getPermissions(),
				s.getVersion(),
				s.getCreatedAt(),
				s.getUpdatedAt());
	}
}
