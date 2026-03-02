package com.akandiah.propmanager.features.organization.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.akandiah.propmanager.features.organization.domain.Organization;

public record UserOrganizationResponse(
		UUID id,
		String name,
		Instant createdAt,
		Instant updatedAt) {

	public static UserOrganizationResponse from(Organization org) {
		return new UserOrganizationResponse(
				org.getId(),
				org.getName(),
				org.getCreatedAt(),
				org.getUpdatedAt());
	}
}
