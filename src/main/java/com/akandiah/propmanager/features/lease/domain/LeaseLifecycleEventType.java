package com.akandiah.propmanager.features.lease.domain;

public enum LeaseLifecycleEventType {
	SUBMITTED_FOR_REVIEW,  // DRAFT → REVIEW; tenants notified to review + sign
	TENANT_SIGNED,         // One tenant signed; manager notified of progress
	TENANT_ALL_SIGNED,     // All tenants signed; REVIEW → SIGNED; manager notified to activate
	ACTIVATED,             // SIGNED → ACTIVE; tenants notified
	EXPIRING_SOON,         // 30-day advance warning; tenants + manager notified
	EXPIRED,               // ACTIVE → EXPIRED (scheduler); tenants + manager notified
	TERMINATED,            // ACTIVE → TERMINATED (manager); tenants notified
	EVICTED,               // ACTIVE → EVICTED (manager); tenants notified
}
