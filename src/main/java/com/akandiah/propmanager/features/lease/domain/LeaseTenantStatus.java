package com.akandiah.propmanager.features.lease.domain;

public enum LeaseTenantStatus {
	/** Invite sent, user has not yet accepted. */
	INVITED,
	/** User accepted the invite and their tenant profile is linked. */
	REGISTERED,
	/** Tenant has signed the lease. */
	SIGNED
}
