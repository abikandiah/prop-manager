package com.akandiah.propmanager.features.membership.api.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateMembershipTemplateRequest(
		@Size(max = 255) String name,

		/** If provided, fully replaces the existing items list. Permissions validated per item. */
		@Valid List<MembershipTemplateItemView> items,

		@NotNull(message = "version is required for optimistic-lock verification") Integer version) {
}
