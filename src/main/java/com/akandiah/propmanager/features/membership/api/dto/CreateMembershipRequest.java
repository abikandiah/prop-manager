package com.akandiah.propmanager.features.membership.api.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CreateMembershipRequest(
		@NotNull(message = "User ID is required") UUID userId) {
}
