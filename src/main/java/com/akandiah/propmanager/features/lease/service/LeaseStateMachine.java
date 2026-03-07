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

	/** REVIEW → SIGNED (auto-triggered when last tenant signs) */
	void advanceToSigned(Lease lease) {
		requireStatus(lease, LeaseStatus.REVIEW, "advance to signed");
		lease.setStatus(LeaseStatus.SIGNED);
	}

	/** SIGNED → ACTIVE */
	void activate(Lease lease) {
		requireStatus(lease, LeaseStatus.SIGNED, "activate");
		lease.setStatus(LeaseStatus.ACTIVE);
	}

	/** REVIEW or SIGNED → DRAFT */
	void revertToDraft(Lease lease) {
		if (lease.getStatus() != LeaseStatus.REVIEW && lease.getStatus() != LeaseStatus.SIGNED) {
			throw new IllegalStateException(
					"Cannot revert to draft: lease " + lease.getId()
							+ " is " + lease.getStatus() + " (expected REVIEW or SIGNED)");
		}
		lease.setStatus(LeaseStatus.DRAFT);
	}

	/** ACTIVE → TERMINATED */
	void terminate(Lease lease) {
		requireStatus(lease, LeaseStatus.ACTIVE, "terminate");
		lease.setStatus(LeaseStatus.TERMINATED);
	}

	/** ACTIVE → EVICTED */
	void evict(Lease lease) {
		requireStatus(lease, LeaseStatus.ACTIVE, "evict");
		lease.setStatus(LeaseStatus.EVICTED);
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
