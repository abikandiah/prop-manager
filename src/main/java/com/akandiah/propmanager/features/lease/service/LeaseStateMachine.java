package com.akandiah.propmanager.features.lease.service;

import org.springframework.stereotype.Component;

import com.akandiah.propmanager.features.lease.domain.Lease;
import com.akandiah.propmanager.features.lease.domain.LeaseStatus;

/**
 * Pure domain component for lease status transitions.
 * Owns guard logic and status mutation only — no I/O.
 * Callers (LeaseService) are responsible for loading and persisting the entity.
 */
@Component
public class LeaseStateMachine {

	/** DRAFT → REVIEW */
	void submitForReview(Lease lease) {
		requireStatus(lease, LeaseStatus.DRAFT, "submit for review");
		lease.setStatus(LeaseStatus.REVIEW);
	}

	/** REVIEW → ACTIVE */
	void activate(Lease lease) {
		requireStatus(lease, LeaseStatus.REVIEW, "activate");
		lease.setStatus(LeaseStatus.ACTIVE);
	}

	/** REVIEW → DRAFT */
	void revertToDraft(Lease lease) {
		requireStatus(lease, LeaseStatus.REVIEW, "revert to draft");
		lease.setStatus(LeaseStatus.DRAFT);
	}

	/** ACTIVE → TERMINATED */
	void terminate(Lease lease) {
		requireStatus(lease, LeaseStatus.ACTIVE, "terminate");
		lease.setStatus(LeaseStatus.TERMINATED);
	}

	// ───────────────────────── Guards ─────────────────────────

	void requireStatus(Lease lease, LeaseStatus expected, String action) {
		if (lease.getStatus() != expected) {
			throw new IllegalStateException(
					"Cannot " + action + ": lease " + lease.getId()
							+ " is " + lease.getStatus() + " (expected " + expected + ")");
		}
	}

	void requireDraft(Lease lease) {
		if (lease.getStatus() != LeaseStatus.DRAFT) {
			throw new IllegalStateException(
					"Lease " + lease.getId() + " is " + lease.getStatus() + "; only DRAFT leases can be modified");
		}
	}
}
