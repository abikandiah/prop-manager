package com.akandiah.propmanager.features.user.api.dto;

/**
 * High-level UI roles derived server-side from a user's access entries and
 * Spring Security authorities. Used by the frontend to determine layout,
 * navigation, and application mode.
 *
 * <p>These are UI hints only — they carry no backend security enforcement.
 * All real authorization is handled by {@code @PreAuthorize} and
 * {@link com.akandiah.propmanager.security.PermissionGuard}.
 */
public enum UiRole {
	/** System admin (ROLE_ADMIN) or organisation admin/owner. */
	ADMIN,
	/** Operational access to properties — property managers and landlords. */
	MANAGER,
	/** Active tenant with lease-level access. */
	TENANT,
	/** Authenticated user with no meaningful access entries. */
	OTHER
}
