package com.akandiah.propmanager.features.organization.api.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateMembershipRequest(
		@NotNull(message = "version is required for optimistic-lock verification") Integer version) {
}
