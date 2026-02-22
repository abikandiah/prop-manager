package com.akandiah.propmanager.features.organization.api.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.akandiah.propmanager.features.organization.domain.MemberScope;
import com.akandiah.propmanager.features.organization.domain.ScopeType;

public record MemberScopeResponse(
		UUID id,
		UUID membershipId,
		ScopeType scopeType,
		UUID scopeId,
		Map<String, String> permissions,
		Integer version,
		Instant createdAt,
		Instant updatedAt) {

	public static MemberScopeResponse from(MemberScope s) {
		return from(s, s.getMembership().getId());
	}

	/** Use when membershipId is already known (e.g. when listing by membership) to avoid N+1. */
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
