package com.akandiah.propmanager.features.membership.api.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CreateMembershipRequest(
		/** Client-supplied entity ID. Null is accepted — the server generates a UUID v7. */
		UUID id,

		@NotNull(message = "User ID is required") UUID userId) {
}
