package com.akandiah.propmanager.features.lease.domain;

import java.util.Set;

public enum LeaseStatus {
	DRAFT,
	REVIEW,
	SIGNED,
	ACTIVE,
	EXPIRED,
	TERMINATED,
	EVICTED;

	/** Statuses that grant a tenant active access rights. */
	public static final Set<LeaseStatus> ACTIVE_STATUSES = Set.of(DRAFT, REVIEW, SIGNED, ACTIVE);
}
