package com.akandiah.propmanager.features.membership.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.akandiah.propmanager.features.membership.domain.MembershipTemplate;

public record MembershipTemplateResponse(
		UUID id,
		/** Organization ID; null for system-wide templates. */
		UUID orgId,
		String name,
		List<MembershipTemplateItemView> items,
		Integer version,
		Instant createdAt,
		Instant updatedAt) {

	public static MembershipTemplateResponse from(MembershipTemplate t) {
		return new MembershipTemplateResponse(
				t.getId(),
				t.getOrg() != null ? t.getOrg().getId() : null,
				t.getName(),
				t.getItems().stream().map(MembershipTemplateItemView::from).toList(),
				t.getVersion(),
				t.getCreatedAt(),
				t.getUpdatedAt());
	}
}
