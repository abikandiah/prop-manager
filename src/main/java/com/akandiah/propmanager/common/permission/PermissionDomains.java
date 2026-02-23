package com.akandiah.propmanager.common.permission;

import java.util.Set;

/**
 * Domain keys for scoped permissions (leases, maintenance, finances).
 * Used in permission JSON and JWT to keep payloads small.
 */
public final class PermissionDomains {

	private PermissionDomains() {
		throw new UnsupportedOperationException("Constants class");
	}

	/** Leases */
	public static final String LEASES = "l";
	/** Maintenance */
	public static final String MAINTENANCE = "m";
	/** Finances */
	public static final String FINANCES = "f";
	/** Tenants (profiles, PII: emergency contacts, vehicle info, notes) */
	public static final String TENANTS = "t";

	/** All valid domain keys for validation. */
	public static final Set<String> VALID_KEYS = Set.of(LEASES, MAINTENANCE, FINANCES, TENANTS);
}
