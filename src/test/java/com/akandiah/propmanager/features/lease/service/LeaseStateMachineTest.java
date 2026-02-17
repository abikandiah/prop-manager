package com.akandiah.propmanager.features.lease.service;

import static com.akandiah.propmanager.TestDataFactory.lease;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.features.lease.api.dto.LeaseResponse;
import com.akandiah.propmanager.features.lease.domain.Lease;
import com.akandiah.propmanager.features.lease.domain.LeaseRepository;
import com.akandiah.propmanager.features.lease.domain.LeaseStatus;

/**
 * Unit tests for {@link LeaseStateMachine}.
 * Tests lease status transitions and state validation.
 */
@ExtendWith(MockitoExtension.class)
class LeaseStateMachineTest {

	@Mock
	private LeaseRepository leaseRepository;

	private LeaseStateMachine stateMachine;

	@BeforeEach
	void setUp() {
		stateMachine = new LeaseStateMachine(leaseRepository);
	}

	// ═══════════════════════════════════════════════════════════════════════
	// Submit for Review (DRAFT → REVIEW)
	// ═══════════════════════════════════════════════════════════════════════

	@Test
	void shouldSubmitDraftLeaseForReview() {
		UUID leaseId = UUID.randomUUID();
		Lease draftLease = lease()
				.id(leaseId)
				.status(LeaseStatus.DRAFT)
				.build();

		when(leaseRepository.findById(leaseId)).thenReturn(Optional.of(draftLease));
		when(leaseRepository.save(any(Lease.class))).thenAnswer(invocation -> invocation.getArgument(0));

		LeaseResponse response = stateMachine.submitForReview(leaseId);

		assertThat(response.status()).isEqualTo(LeaseStatus.REVIEW);
		verify(leaseRepository).save(draftLease);
		assertThat(draftLease.getStatus()).isEqualTo(LeaseStatus.REVIEW);
	}

	@Test
	void shouldNotSubmitNonDraftLeaseForReview() {
		UUID leaseId = UUID.randomUUID();
		Lease activeLease = lease()
				.id(leaseId)
				.status(LeaseStatus.ACTIVE)
				.build();

		when(leaseRepository.findById(leaseId)).thenReturn(Optional.of(activeLease));

		assertThatThrownBy(() -> stateMachine.submitForReview(leaseId))
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
		UUID leaseId = UUID.randomUUID();
		Lease pendingLease = lease()
				.id(leaseId)
				.status(LeaseStatus.REVIEW)
				.build();

		when(leaseRepository.findById(leaseId)).thenReturn(Optional.of(pendingLease));
		when(leaseRepository.save(any(Lease.class))).thenAnswer(invocation -> invocation.getArgument(0));

		LeaseResponse response = stateMachine.activate(leaseId);

		assertThat(response.status()).isEqualTo(LeaseStatus.ACTIVE);
		verify(leaseRepository).save(pendingLease);
		assertThat(pendingLease.getStatus()).isEqualTo(LeaseStatus.ACTIVE);
	}

	@Test
	void shouldNotActivateDraftLease() {
		UUID leaseId = UUID.randomUUID();
		Lease draftLease = lease()
				.id(leaseId)
				.status(LeaseStatus.DRAFT)
				.build();

		when(leaseRepository.findById(leaseId)).thenReturn(Optional.of(draftLease));

		assertThatThrownBy(() -> stateMachine.activate(leaseId))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Cannot activate")
				.hasMessageContaining("is DRAFT")
				.hasMessageContaining("expected REVIEW");
	}

	@Test
	void shouldNotActivateAlreadyActiveLease() {
		UUID leaseId = UUID.randomUUID();
		Lease activeLease = lease()
				.id(leaseId)
				.status(LeaseStatus.ACTIVE)
				.build();

		when(leaseRepository.findById(leaseId)).thenReturn(Optional.of(activeLease));

		assertThatThrownBy(() -> stateMachine.activate(leaseId))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("is ACTIVE");
	}

	// ═══════════════════════════════════════════════════════════════════════
	// Revert to Draft (REVIEW → DRAFT)
	// ═══════════════════════════════════════════════════════════════════════

	@Test
	void shouldRevertPendingReviewLeaseToDraft() {
		UUID leaseId = UUID.randomUUID();
		Lease pendingLease = lease()
				.id(leaseId)
				.status(LeaseStatus.REVIEW)
				.build();

		when(leaseRepository.findById(leaseId)).thenReturn(Optional.of(pendingLease));
		when(leaseRepository.save(any(Lease.class))).thenAnswer(invocation -> invocation.getArgument(0));

		LeaseResponse response = stateMachine.revertToDraft(leaseId);

		assertThat(response.status()).isEqualTo(LeaseStatus.DRAFT);
		verify(leaseRepository).save(pendingLease);
		assertThat(pendingLease.getStatus()).isEqualTo(LeaseStatus.DRAFT);
	}

	@Test
	void shouldNotRevertActiveLeaseToDraft() {
		UUID leaseId = UUID.randomUUID();
		Lease activeLease = lease()
				.id(leaseId)
				.status(LeaseStatus.ACTIVE)
				.build();

		when(leaseRepository.findById(leaseId)).thenReturn(Optional.of(activeLease));

		assertThatThrownBy(() -> stateMachine.revertToDraft(leaseId))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Cannot revert to draft")
				.hasMessageContaining("is ACTIVE");
	}

	// ═══════════════════════════════════════════════════════════════════════
	// Terminate (ACTIVE → TERMINATED)
	// ═══════════════════════════════════════════════════════════════════════

	@Test
	void shouldTerminateActiveLease() {
		UUID leaseId = UUID.randomUUID();
		Lease activeLease = lease()
				.id(leaseId)
				.status(LeaseStatus.ACTIVE)
				.build();

		when(leaseRepository.findById(leaseId)).thenReturn(Optional.of(activeLease));
		when(leaseRepository.save(any(Lease.class))).thenAnswer(invocation -> invocation.getArgument(0));

		LeaseResponse response = stateMachine.terminate(leaseId);

		assertThat(response.status()).isEqualTo(LeaseStatus.TERMINATED);
		verify(leaseRepository).save(activeLease);
		assertThat(activeLease.getStatus()).isEqualTo(LeaseStatus.TERMINATED);
	}

	@Test
	void shouldNotTerminateDraftLease() {
		UUID leaseId = UUID.randomUUID();
		Lease draftLease = lease()
				.id(leaseId)
				.status(LeaseStatus.DRAFT)
				.build();

		when(leaseRepository.findById(leaseId)).thenReturn(Optional.of(draftLease));

		assertThatThrownBy(() -> stateMachine.terminate(leaseId))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Cannot terminate")
				.hasMessageContaining("is DRAFT")
				.hasMessageContaining("expected ACTIVE");
	}

	@Test
	void shouldNotTerminatePendingReviewLease() {
		UUID leaseId = UUID.randomUUID();
		Lease pendingLease = lease()
				.id(leaseId)
				.status(LeaseStatus.REVIEW)
				.build();

		when(leaseRepository.findById(leaseId)).thenReturn(Optional.of(pendingLease));

		assertThatThrownBy(() -> stateMachine.terminate(leaseId))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("is REVIEW");
	}

	// ═══════════════════════════════════════════════════════════════════════
	// RequireDraft Helper
	// ═══════════════════════════════════════════════════════════════════════

	@Test
	void shouldAllowOperationOnDraftLease() {
		Lease draftLease = lease()
				.status(LeaseStatus.DRAFT)
				.build();

		// Should not throw
		stateMachine.requireDraft(draftLease);
	}

	@Test
	void shouldRejectOperationOnNonDraftLease() {
		UUID leaseId = UUID.randomUUID();
		Lease activeLease = lease()
				.id(leaseId)
				.status(LeaseStatus.ACTIVE)
				.build();

		assertThatThrownBy(() -> stateMachine.requireDraft(activeLease))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Lease " + leaseId + " is ACTIVE")
				.hasMessageContaining("only DRAFT leases can be modified");
	}

	@Test
	void shouldRejectOperationOnTerminatedLease() {
		UUID leaseId = UUID.randomUUID();
		Lease terminatedLease = lease()
				.id(leaseId)
				.status(LeaseStatus.TERMINATED)
				.build();

		assertThatThrownBy(() -> stateMachine.requireDraft(terminatedLease))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("is TERMINATED");
	}

	// ═══════════════════════════════════════════════════════════════════════
	// GetEntity Helper
	// ═══════════════════════════════════════════════════════════════════════

	@Test
	void shouldGetEntityById() {
		UUID leaseId = UUID.randomUUID();
		Lease lease = lease().id(leaseId).build();

		when(leaseRepository.findById(leaseId)).thenReturn(Optional.of(lease));

		Lease result = stateMachine.getEntity(leaseId);

		assertThat(result).isEqualTo(lease);
	}

	@Test
	void shouldThrowResourceNotFoundExceptionWhenLeaseNotFound() {
		UUID leaseId = UUID.randomUUID();

		when(leaseRepository.findById(leaseId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> stateMachine.getEntity(leaseId))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Lease")
				.hasMessageContaining(leaseId.toString())
				.hasMessageContaining("not found");
	}

	// ═══════════════════════════════════════════════════════════════════════
	// State Transition Integration
	// ═══════════════════════════════════════════════════════════════════════

	@Test
	void shouldFollowCompleteStateTransitionFlow() {
		UUID leaseId = UUID.randomUUID();
		Lease lease = lease()
				.id(leaseId)
				.status(LeaseStatus.DRAFT)
				.build();

		when(leaseRepository.findById(leaseId)).thenReturn(Optional.of(lease));
		when(leaseRepository.save(any(Lease.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// DRAFT → REVIEW
		stateMachine.submitForReview(leaseId);
		assertThat(lease.getStatus()).isEqualTo(LeaseStatus.REVIEW);

		// REVIEW → ACTIVE
		stateMachine.activate(leaseId);
		assertThat(lease.getStatus()).isEqualTo(LeaseStatus.ACTIVE);

		// ACTIVE → TERMINATED
		stateMachine.terminate(leaseId);
		assertThat(lease.getStatus()).isEqualTo(LeaseStatus.TERMINATED);
	}

	@Test
	void shouldAllowRevertDuringReview() {
		UUID leaseId = UUID.randomUUID();
		Lease lease = lease()
				.id(leaseId)
				.status(LeaseStatus.DRAFT)
				.build();

		when(leaseRepository.findById(leaseId)).thenReturn(Optional.of(lease));
		when(leaseRepository.save(any(Lease.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// DRAFT → REVIEW
		stateMachine.submitForReview(leaseId);
		assertThat(lease.getStatus()).isEqualTo(LeaseStatus.REVIEW);

		// REVIEW → DRAFT (revert)
		stateMachine.revertToDraft(leaseId);
		assertThat(lease.getStatus()).isEqualTo(LeaseStatus.DRAFT);

		// Can submit again
		stateMachine.submitForReview(leaseId);
		assertThat(lease.getStatus()).isEqualTo(LeaseStatus.REVIEW);
	}
}
