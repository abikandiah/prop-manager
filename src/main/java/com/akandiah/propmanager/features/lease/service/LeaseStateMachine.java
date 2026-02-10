package com.akandiah.propmanager.features.lease.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.features.lease.api.dto.LeaseResponse;
import com.akandiah.propmanager.features.lease.domain.Lease;
import com.akandiah.propmanager.features.lease.domain.LeaseRepository;
import com.akandiah.propmanager.features.lease.domain.LeaseStatus;

/**
 * Handles lease status transitions.
 */
@Service
public class LeaseStateMachine {

	private final LeaseRepository leaseRepository;

	public LeaseStateMachine(LeaseRepository leaseRepository) {
		this.leaseRepository = leaseRepository;
	}

	/** Owner sends the draft to the tenant for review. */
	@Transactional
	public LeaseResponse submitForReview(UUID id) {
		Lease lease = getEntity(id);
		requireStatus(lease, LeaseStatus.DRAFT, "submit for review");
		lease.setStatus(LeaseStatus.PENDING_REVIEW);
		lease = leaseRepository.save(lease);
		return LeaseResponse.from(lease);
	}

	/** Both parties signed — lease becomes active and read-only. */
	@Transactional
	public LeaseResponse activate(UUID id) {
		Lease lease = getEntity(id);
		requireStatus(lease, LeaseStatus.PENDING_REVIEW, "activate");
		lease.setStatus(LeaseStatus.ACTIVE);
		lease = leaseRepository.save(lease);
		return LeaseResponse.from(lease);
	}

	/** Revert a pending-review lease back to draft for further edits. */
	@Transactional
	public LeaseResponse revertToDraft(UUID id) {
		Lease lease = getEntity(id);
		requireStatus(lease, LeaseStatus.PENDING_REVIEW, "revert to draft");
		lease.setStatus(LeaseStatus.DRAFT);
		lease = leaseRepository.save(lease);
		return LeaseResponse.from(lease);
	}

	/** Terminate an active lease early. */
	@Transactional
	public LeaseResponse terminate(UUID id) {
		Lease lease = getEntity(id);
		requireStatus(lease, LeaseStatus.ACTIVE, "terminate");
		lease.setStatus(LeaseStatus.TERMINATED);
		lease = leaseRepository.save(lease);
		return LeaseResponse.from(lease);
	}

	// ───────────────────────── Package-private helpers ─────────────────────────

	Lease getEntity(UUID id) {
		return leaseRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Lease", id));
	}

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
