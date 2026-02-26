package com.akandiah.propmanager.common.permission;

import java.util.Set;

/** Domain keys for scoped permissions. Used in permission JSON and JWT. */
public final class PermissionDomains {

	private PermissionDomains() {
		throw new UnsupportedOperationException("Constants class");
	}

	public static final String LEASES = "l";
	public static final String MAINTENANCE = "m";
	public static final String FINANCES = "f";
	public static final String TENANTS = "t";
	public static final String ORGANIZATION = "o";
	public static final String PORTFOLIO = "p";

	public static final Set<String> VALID_KEYS = Set.of(LEASES, MAINTENANCE, FINANCES, TENANTS, ORGANIZATION, PORTFOLIO);
}
