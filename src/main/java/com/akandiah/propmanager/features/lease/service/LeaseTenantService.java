package com.akandiah.propmanager.features.lease.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.features.invite.api.dto.InviteResponse;
import com.akandiah.propmanager.features.invite.domain.Invite;
import com.akandiah.propmanager.features.invite.domain.InviteAcceptedEvent;
import com.akandiah.propmanager.features.invite.domain.InviteRepository;
import com.akandiah.propmanager.features.invite.domain.InviteStatus;
import com.akandiah.propmanager.features.invite.domain.TargetType;
import com.akandiah.propmanager.features.invite.service.InviteService;
import com.akandiah.propmanager.features.lease.api.dto.InviteLeaseTenantRequest;
import com.akandiah.propmanager.features.lease.api.dto.LeaseTenantResponse;
import com.akandiah.propmanager.features.lease.domain.Lease;
import com.akandiah.propmanager.features.lease.domain.LeaseTenant;
import com.akandiah.propmanager.features.lease.domain.LeaseTenantRepository;
import com.akandiah.propmanager.features.lease.domain.LeaseRepository;
import com.akandiah.propmanager.features.lease.domain.LeaseStatus;
import com.akandiah.propmanager.features.tenant.domain.Tenant;
import com.akandiah.propmanager.features.tenant.domain.TenantRepository;
import com.akandiah.propmanager.features.user.domain.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeaseTenantService {

	private final LeaseTenantRepository leaseTenantRepository;
	private final LeaseRepository leaseRepository;
	private final InviteService inviteService;
	private final InviteRepository inviteRepository;
	private final TenantRepository tenantRepository;

	// ───────────────────────── Queries ─────────────────────────

	public List<LeaseTenantResponse> findByLeaseId(UUID leaseId) {
		return leaseTenantRepository.findByLease_Id(leaseId).stream()
				.map(LeaseTenantResponse::from)
				.toList();
	}

	// ───────────────────────── Invite ─────────────────────────

	/**
	 * Invites one or more people to join a DRAFT lease as tenants.
	 * Creates an {@link Invite} for each entry (which sends the invite email)
	 * and a corresponding {@link LeaseTenant} row linking the invite to the lease.
	 * The {@code tenant_id} is null until the invitee accepts.
	 */
	@Transactional
	public List<LeaseTenantResponse> inviteTenants(UUID leaseId, InviteLeaseTenantRequest request, User invitedBy) {
		Lease lease = leaseRepository.findById(leaseId)
				.orElseThrow(() -> new ResourceNotFoundException("Lease", leaseId));

		if (lease.getStatus() != LeaseStatus.DRAFT) {
			throw new IllegalStateException("Tenants can only be invited to DRAFT leases");
		}

		List<LeaseTenant> created = new ArrayList<>();

		for (InviteLeaseTenantRequest.TenantInviteEntry entry : request.invites()) {
			InviteResponse inviteResponse = inviteService.createAndSendInvite(
					entry.email(),
					TargetType.LEASE,
					leaseId,
					entry.role().name(),
					invitedBy,
					Map.of("leaseId", leaseId.toString()));

			// Fetch the entity so we can set the FK on LeaseTenant
			Invite invite = inviteRepository.findById(inviteResponse.id())
					.orElseThrow(() -> new IllegalStateException("Invite not found after creation: " + inviteResponse.id()));

			LeaseTenant leaseTenant = LeaseTenant.builder()
					.lease(lease)
					.invite(invite)
					.role(entry.role())
					.invitedDate(LocalDate.now())
					.build();

			created.add(leaseTenantRepository.save(leaseTenant));
		}

		log.info("Invited {} tenant(s) to lease {}", created.size(), leaseId);
		return created.stream().map(LeaseTenantResponse::from).toList();
	}

	// ───────────────────────── Resend ─────────────────────────

	/**
	 * Resends the invite for a lease tenant slot that is still in INVITED status.
	 * Delegates cooldown and expiry renewal logic to {@link InviteService#resendInvite}.
	 */
	@Transactional
	public LeaseTenantResponse resendTenantInvite(UUID leaseId, UUID leaseTenantId) {
		LeaseTenant leaseTenant = leaseTenantRepository.findById(leaseTenantId)
				.orElseThrow(() -> new ResourceNotFoundException("LeaseTenant", leaseTenantId));

		if (!leaseTenant.getLease().getId().equals(leaseId)) {
			throw new ResourceNotFoundException("LeaseTenant", leaseTenantId);
		}

		if (leaseTenant.getTenant() != null) {
			throw new IllegalStateException("This tenant has already accepted their invitation");
		}

		inviteService.resendInvite(leaseTenant.getInvite().getId(), Map.of("leaseId", leaseId.toString()));

		// Reload to pick up the updated lastResentAt from the invite
		LeaseTenant refreshed = leaseTenantRepository.findById(leaseTenantId)
				.orElseThrow(() -> new ResourceNotFoundException("LeaseTenant", leaseTenantId));

		log.info("Resent invite for LeaseTenant {} on lease {}", leaseTenantId, leaseId);
		return LeaseTenantResponse.from(refreshed);
	}

	// ───────────────────────── Remove ─────────────────────────

	/**
	 * Removes a tenant slot from a DRAFT lease.
	 * Guards: lease must be DRAFT; tenant must not have signed.
	 * Also revokes the associated invite if it is still pending.
	 */
	@Transactional
	public void removeTenant(UUID leaseId, UUID leaseTenantId) {
		Lease lease = leaseRepository.findById(leaseId)
				.orElseThrow(() -> new ResourceNotFoundException("Lease", leaseId));

		if (lease.getStatus() != LeaseStatus.DRAFT) {
			throw new IllegalStateException("Tenants can only be removed from DRAFT leases");
		}

		LeaseTenant leaseTenant = leaseTenantRepository.findById(leaseTenantId)
				.orElseThrow(() -> new ResourceNotFoundException("LeaseTenant", leaseTenantId));

		if (!leaseTenant.getLease().getId().equals(leaseId)) {
			throw new ResourceNotFoundException("LeaseTenant", leaseTenantId);
		}

		if (leaseTenant.getSignedDate() != null) {
			throw new IllegalStateException("Cannot remove a tenant who has already signed the lease");
		}

		// Revoke the invite so it can no longer be accepted
		if (leaseTenant.getInvite().getStatus() == InviteStatus.PENDING) {
			inviteService.revokeInvite(leaseTenant.getInvite().getId());
		}

		leaseTenantRepository.delete(leaseTenant);
		log.info("Removed LeaseTenant {} from lease {}", leaseTenantId, leaseId);
	}

	// ───────────────────────── Invite acceptance ─────────────────────────

	/**
	 * Reacts to a redeemed invite. If the invite targets a LEASE,
	 * finds the corresponding {@link LeaseTenant} slot, creates a {@link Tenant}
	 * profile for the user if they don't have one yet, and links it.
	 * Runs within the same transaction as {@link InviteService#redeemInvite} (BEFORE_COMMIT).
	 */
	@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
	public void onInviteAccepted(InviteAcceptedEvent event) {
		Invite invite = event.invite();
		if (invite.getTargetType() != TargetType.LEASE) {
			return;
		}

		leaseTenantRepository.findByInvite_Id(invite.getId()).ifPresent(leaseTenant -> {
			User claimedUser = event.claimedUser();

			Tenant tenant = tenantRepository.findByUser_Id(claimedUser.getId())
					.orElseGet(() -> {
						log.info("Creating tenant profile for user {} on invite acceptance", claimedUser.getId());
						return tenantRepository.save(Tenant.builder().user(claimedUser).build());
					});

			leaseTenant.setTenant(tenant);
			leaseTenantRepository.save(leaseTenant);

			log.info("Linked tenant {} to LeaseTenant {} via invite {}", tenant.getId(), leaseTenant.getId(),
					invite.getId());
		});
	}
}
