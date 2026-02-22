package com.akandiah.propmanager.features.organization.api.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.akandiah.propmanager.features.organization.domain.Organization;

public record OrganizationResponse(
		UUID id,
		String name,
		String taxId,
		Map<String, Object> settings,
		Integer version,
		Instant createdAt,
		Instant updatedAt) {

	public static OrganizationResponse from(Organization org) {
		return new OrganizationResponse(
				org.getId(),
				org.getName(),
				org.getTaxId(),
				org.getSettings(),
				org.getVersion(),
				org.getCreatedAt(),
				org.getUpdatedAt());
	}
}
