package com.akandiah.propmanager.features.user.api.dto;

/**
 * Request body for PATCH /api/me (e.g. accept terms).
 */
public record PatchMeRequest(Boolean termsAccepted) {
}
