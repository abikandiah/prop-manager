package com.akandiah.propmanager.features.membership.api.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record CreateMembershipTemplateRequest(
		/** Client-supplied entity ID. Null is accepted — the server generates a UUID v7. */
		UUID id,

		@NotBlank(message = "Name is required") @Size(max = 255) String name,

		/** Organization ID; null for system-wide template (requires ADMIN role). */
		UUID orgId,

		/** At least one scope-level item is required. Permissions validated per item. */
		@NotEmpty(message = "At least one template item is required") @Valid List<MembershipTemplateItemView> items) {
}
