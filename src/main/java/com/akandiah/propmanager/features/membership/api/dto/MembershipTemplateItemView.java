package com.akandiah.propmanager.features.membership.api.dto;

import java.util.Map;

import com.akandiah.propmanager.common.permission.ResourceType;
import com.akandiah.propmanager.features.membership.domain.MembershipTemplateItem;

import jakarta.validation.constraints.NotNull;

/**
 * Represents one scope-level entry in a membership template.
 * Used in both request bodies and responses.
 */
public record MembershipTemplateItemView(
		@NotNull(message = "scopeType is required") ResourceType scopeType,
		@NotNull(message = "permissions is required") Map<String, String> permissions) {

	public static MembershipTemplateItemView from(MembershipTemplateItem item) {
		return new MembershipTemplateItemView(item.getScopeType(), item.getPermissions());
	}

	public MembershipTemplateItem toEntity() {
		return MembershipTemplateItem.builder()
				.scopeType(scopeType)
				.permissions(permissions)
				.build();
	}
}
