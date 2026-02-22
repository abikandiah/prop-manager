package com.akandiah.propmanager.features.organization.api.dto;

import com.akandiah.propmanager.features.organization.domain.Role;

import jakarta.validation.constraints.NotNull;

public record UpdateMembershipRequest(
		@NotNull(message = "Role is required") Role role,
		@NotNull(message = "version is required for optimistic-lock verification") Integer version) {
}
