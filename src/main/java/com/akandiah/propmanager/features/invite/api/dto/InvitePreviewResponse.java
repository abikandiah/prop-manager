package com.akandiah.propmanager.features.invite.api.dto;

import java.time.Instant;
import java.util.Map;

import com.akandiah.propmanager.features.invite.domain.InviteStatus;
import com.akandiah.propmanager.features.invite.domain.TargetType;

/**
 * Public-safe invite preview returned by the unauthenticated token-lookup endpoint.
 * Email is masked; no internal IDs or sensitive fields are exposed.
 *
 * <p>The {@code preview} map contains domain-specific snapshot data stored in
 * {@code invite.attributes["preview"]} at creation time. Each domain owns the
 * keys it writes (e.g. LEASE writes {@code property}, {@code unit}, {@code lease};
 * MEMBERSHIP writes {@code organizationName}).
 */
public record InvitePreviewResponse(
		String maskedEmail,
		InviteStatus status,
		boolean isValid,
		boolean isExpired,
		Instant expiresAt,
		String invitedByName,
		TargetType targetType,
		Map<String, Object> preview) {
}
