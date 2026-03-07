package com.akandiah.propmanager.features.organization.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.akandiah.propmanager.features.organization.domain.Organization;
import com.akandiah.propmanager.features.user.api.dto.UiRole;

public record UserOrganizationResponse(
		UUID id,
		String name,
		List<UiRole> roles,
		Instant createdAt,
		Instant updatedAt) {

	public static UserOrganizationResponse from(Organization org, List<UiRole> roles) {
		return new UserOrganizationResponse(
				org.getId(),
				org.getName(),
				roles,
				org.getCreatedAt(),
				org.getUpdatedAt());
	}
}
