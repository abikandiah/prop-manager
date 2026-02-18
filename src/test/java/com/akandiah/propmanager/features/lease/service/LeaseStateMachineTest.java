package com.akandiah.propmanager.features.lease.service;

import static com.akandiah.propmanager.TestDataFactory.lease;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.akandiah.propmanager.features.lease.domain.Lease;
import com.akandiah.propmanager.features.lease.domain.LeaseStatus;

/**
 * Unit tests for {@link LeaseStateMachine}.
 * The state machine is a pure domain component — no repository, no I/O.
 * Tests mutate entities in-place and verify the resulting status.
 */
class LeaseStateMachineTest {

	private final LeaseStateMachine stateMachine = new LeaseStateMachine();

	// ═══════════════════════════════════════════════════════════════════════
	// Submit for Review (DRAFT → REVIEW)
	// ═══════════════════════════════════════════════════════════════════════

	@Test
	void shouldSubmitDraftLeaseForReview() {
		Lease lease = lease().status(LeaseStatus.DRAFT).build();

		stateMachine.submitForReview(lease);

		assertThat(lease.getStatus()).isEqualTo(LeaseStatus.REVIEW);
	}

	@Test
	void shouldNotSubmitNonDraftLeaseForReview() {
		UUID leaseId = UUID.randomUUID();
		Lease lease = lease().id(leaseId).status(LeaseStatus.ACTIVE).build();

		assertThatThrownBy(() -> stateMachine.submitForReview(lease))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Cannot submit for review")
				.hasMessageContaining(leaseId.toString())
				.hasMessageContaining("is ACTIVE")
				.hasMessageContaining("expected DRAFT");
	}

	// ═══════════════════════════════════════════════════════════════════════
	// Activate (REVIEW → ACTIVE)
	// ═══════════════════════════════════════════════════════════════════════

	@Test
	void shouldActivatePendingReviewLease() {
		Lease lease = lease().status(LeaseStatus.REVIEW).build();

		stateMachine.activate(lease);

		assertThat(lease.getStatus()).isEqualTo(LeaseStatus.ACTIVE);
	}

	@Test
	void shouldNotActivateDraftLease() {
		UUID leaseId = UUID.randomUUID();
		Lease lease = lease().id(leaseId).status(LeaseStatus.DRAFT).build();

		assertThatThrownBy(() -> stateMachine.activate(lease))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Cannot activate")
				.hasMessageContaining("is DRAFT")
				.hasMessageContaining("expected REVIEW");
	}

	@Test
	void shouldNotActivateAlreadyActiveLease() {
		UUID leaseId = UUID.randomUUID();
		Lease lease = lease().id(leaseId).status(LeaseStatus.ACTIVE).build();

		assertThatThrownBy(() -> stateMachine.activate(lease))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("is ACTIVE");
	}

	// ═══════════════════════════════════════════════════════════════════════
	// Revert to Draft (REVIEW → DRAFT)
	// ═══════════════════════════════════════════════════════════════════════

	@Test
	void shouldRevertPendingReviewLeaseToDraft() {
		Lease lease = lease().status(LeaseStatus.REVIEW).build();

		stateMachine.revertToDraft(lease);

		assertThat(lease.getStatus()).isEqualTo(LeaseStatus.DRAFT);
	}

	@Test
	void shouldNotRevertActiveLeaseToDraft() {
		UUID leaseId = UUID.randomUUID();
		Lease lease = lease().id(leaseId).status(LeaseStatus.ACTIVE).build();

		assertThatThrownBy(() -> stateMachine.revertToDraft(lease))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Cannot revert to draft")
				.hasMessageContaining("is ACTIVE");
	}

	// ═══════════════════════════════════════════════════════════════════════
	// Terminate (ACTIVE → TERMINATED)
	// ═══════════════════════════════════════════════════════════════════════

	@Test
	void shouldTerminateActiveLease() {
		Lease lease = lease().status(LeaseStatus.ACTIVE).build();

		stateMachine.terminate(lease);

		assertThat(lease.getStatus()).isEqualTo(LeaseStatus.TERMINATED);
	}

	@Test
	void shouldNotTerminateDraftLease() {
		UUID leaseId = UUID.randomUUID();
		Lease lease = lease().id(leaseId).status(LeaseStatus.DRAFT).build();

		assertThatThrownBy(() -> stateMachine.terminate(lease))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Cannot terminate")
				.hasMessageContaining("is DRAFT")
				.hasMessageContaining("expected ACTIVE");
	}

	@Test
	void shouldNotTerminatePendingReviewLease() {
		UUID leaseId = UUID.randomUUID();
		Lease lease = lease().id(leaseId).status(LeaseStatus.REVIEW).build();

		assertThatThrownBy(() -> stateMachine.terminate(lease))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("is REVIEW");
	}

	// ═══════════════════════════════════════════════════════════════════════
	// RequireDraft Helper
	// ═══════════════════════════════════════════════════════════════════════

	@Test
	void shouldAllowOperationOnDraftLease() {
		Lease lease = lease().status(LeaseStatus.DRAFT).build();

		// Should not throw
		stateMachine.requireDraft(lease);
	}

	@Test
	void shouldRejectOperationOnNonDraftLease() {
		UUID leaseId = UUID.randomUUID();
		Lease lease = lease().id(leaseId).status(LeaseStatus.ACTIVE).build();

		assertThatThrownBy(() -> stateMachine.requireDraft(lease))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Lease " + leaseId + " is ACTIVE")
				.hasMessageContaining("only DRAFT leases can be modified");
	}

	@Test
	void shouldRejectOperationOnTerminatedLease() {
		UUID leaseId = UUID.randomUUID();
		Lease lease = lease().id(leaseId).status(LeaseStatus.TERMINATED).build();

		assertThatThrownBy(() -> stateMachine.requireDraft(lease))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("is TERMINATED");
	}

	// ═══════════════════════════════════════════════════════════════════════
	// State Transition Integration
	// ═══════════════════════════════════════════════════════════════════════

	@Test
	void shouldFollowCompleteStateTransitionFlow() {
		Lease lease = lease().status(LeaseStatus.DRAFT).build();

		stateMachine.submitForReview(lease);
		assertThat(lease.getStatus()).isEqualTo(LeaseStatus.REVIEW);

		stateMachine.activate(lease);
		assertThat(lease.getStatus()).isEqualTo(LeaseStatus.ACTIVE);

		stateMachine.terminate(lease);
		assertThat(lease.getStatus()).isEqualTo(LeaseStatus.TERMINATED);
	}

	@Test
	void shouldAllowRevertDuringReview() {
		Lease lease = lease().status(LeaseStatus.DRAFT).build();

		stateMachine.submitForReview(lease);
		assertThat(lease.getStatus()).isEqualTo(LeaseStatus.REVIEW);

		stateMachine.revertToDraft(lease);
		assertThat(lease.getStatus()).isEqualTo(LeaseStatus.DRAFT);

		// Can submit again
		stateMachine.submitForReview(lease);
		assertThat(lease.getStatus()).isEqualTo(LeaseStatus.REVIEW);
	}
}
