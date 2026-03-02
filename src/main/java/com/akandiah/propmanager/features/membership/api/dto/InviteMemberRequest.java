package com.akandiah.propmanager.features.membership.api.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InviteMemberRequest(
		@NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email,
		@NotNull(message = "At least one permission assignment is required")
		@Size(min = 1, message = "At least one permission assignment is required")
		@Valid List<CreatePolicyAssignmentRequest> assignments) {
}
