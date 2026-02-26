package com.akandiah.propmanager.features.membership.api.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.akandiah.propmanager.common.permission.ResourceType;

import jakarta.validation.constraints.NotNull;

/**
 * Request body for {@code POST /api/memberships/{id}/apply-template}.
 *
 * <p>Sets the membership's template and creates binding {@code MemberScope} rows for
 * each resource ID supplied. Existing scopes are preserved (new binding rows are created
 * only where none already exist for the given scope).
 *
 * @param templateId  the template to link to the membership
 * @param resourceIds optional map of scope type → list of resource IDs to bind;
 *                    e.g. {@code { PROPERTY: [uuid1, uuid2] }}
 */
public record ApplyTemplateRequest(
		@NotNull(message = "templateId is required") UUID templateId,
		Map<ResourceType, List<UUID>> resourceIds) {
}
