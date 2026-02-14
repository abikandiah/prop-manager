package com.akandiah.propmanager.features.invite.api.dto;

import java.util.Map;
import java.util.UUID;

import com.akandiah.propmanager.features.invite.domain.TargetType;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request to create a new invitation.
 */
public record CreateInviteRequest(
		@NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email,

		@NotNull(message = "Target type is required") TargetType targetType,

		@NotNull(message = "Target ID is required") UUID targetId,

		@NotBlank(message = "Role is required") String role,

		Map<String, Object> metadata) {
}
