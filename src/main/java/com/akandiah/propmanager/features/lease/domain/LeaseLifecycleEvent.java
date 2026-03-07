package com.akandiah.propmanager.features.lease.domain;

import java.util.UUID;

public record LeaseLifecycleEvent(UUID leaseId, LeaseLifecycleEventType type, UUID subjectId) {

	/** Convenience constructor for events with no subject (most lifecycle events). */
	public LeaseLifecycleEvent(UUID leaseId, LeaseLifecycleEventType type) {
		this(leaseId, type, null);
	}
}
