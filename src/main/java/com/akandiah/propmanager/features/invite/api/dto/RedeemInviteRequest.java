package com.akandiah.propmanager.features.invite.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to redeem/accept an invitation.
 */
public record RedeemInviteRequest(
		@NotBlank(message = "Token is required") String token,

		@NotBlank(message = "Name is required") String name,

		@NotBlank(message = "Password is required") String password) {
}
