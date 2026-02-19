package com.akandiah.propmanager.features.lease.api.dto;

import java.util.List;

import com.akandiah.propmanager.features.lease.domain.LeaseTenantRole;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record InviteLeaseTenantRequest(
		@NotEmpty(message = "At least one invite entry is required") List<@Valid TenantInviteEntry> invites) {

	public record TenantInviteEntry(
			@NotBlank(message = "Email is required") @Email(message = "Must be a valid email") String email,
			@NotNull(message = "Role is required") LeaseTenantRole role) {
	}
}
