package com.akandiah.propmanager.features.membership.api.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record InviteMemberRequest(
		@NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email,
		@Valid List<CreatePolicyAssignmentRequest> assignments) {
}
