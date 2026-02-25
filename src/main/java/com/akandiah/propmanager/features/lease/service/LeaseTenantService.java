package com.akandiah.propmanager.features.lease.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.features.auth.domain.PermissionsChangedEvent;
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
import com.akandiah.propmanager.features.lease.domain.LeaseTenantRole;
import com.akandiah.propmanager.features.lease.domain.LeaseRepository;
import com.akandiah.propmanager.features.lease.domain.LeaseStatus;
import com.akandiah.propmanager.features.prop.domain.Address;
import com.akandiah.propmanager.features.prop.domain.Prop;
import com.akandiah.propmanager.features.unit.domain.Unit;
import com.akandiah.propmanager.common.notification.NotificationReferenceType;
import com.akandiah.propmanager.features.notification.domain.NotificationDelivery;
import com.akandiah.propmanager.features.notification.domain.NotificationDeliveryRepository;
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

	// Key constants for invite attributes owned by this domain
	private static final String ATTR_ROLE = "role";
	private static final String ATTR_PREVIEW = "preview";

	private final LeaseTenantRepository leaseTenantRepository;
	private final LeaseRepository leaseRepository;
	private final InviteService inviteService;
	private final InviteRepository inviteRepository;
	private final TenantRepository tenantRepository;
	private final NotificationDeliveryRepository deliveryRepository;
	private final ApplicationEventPublisher eventPublisher;

	// ───────────────────────── Queries ─────────────────────────

	public List<LeaseTenantResponse> findByLeaseId(UUID leaseId) {
		List<LeaseTenant> tenants = leaseTenantRepository.findByLease_Id(leaseId);

		// Batch-fetch the latest email delivery for each invite to avoid N+1
		List<UUID> inviteIds = tenants.stream()
				.map(lt -> lt.getInvite().getId())
				.toList();
		Map<UUID, NotificationDelivery> latestByInvite = deliveryRepository
				.findLatestByReferenceTypeAndReferenceIdIn(NotificationReferenceType.INVITE, inviteIds)
				.stream()
				.collect(Collectors.toMap(NotificationDelivery::getReferenceId, d -> d));

		return tenants.stream()
				.map(lt -> LeaseTenantResponse.from(lt, latestByInvite.get(lt.getInvite().getId())))
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
		Lease lease = leaseRepository.findByIdWithUnitPropertyAndAddress(leaseId)
				.orElseThrow(() -> new ResourceNotFoundException("Lease", leaseId));

		if (lease.getStatus() != LeaseStatus.DRAFT) {
			throw new IllegalStateException("Tenants can only be invited to DRAFT leases");
		}

		// Build the preview snapshot once — shared by all invitees on this lease
		Map<String, Object> leasePreviewSnapshot = buildPreviewSnapshot(lease);

		List<LeaseTenant> created = new ArrayList<>();

		for (InviteLeaseTenantRequest.TenantInviteEntry entry : request.invites()) {
			Map<String, Object> attributes = new HashMap<>();
			attributes.put(ATTR_ROLE, entry.role().name());
			attributes.put("leaseId", leaseId.toString());
			attributes.put(ATTR_PREVIEW, leasePreviewSnapshot);

			InviteResponse inviteResponse = inviteService.createAndSendInvite(
					entry.email(),
					TargetType.LEASE,
					leaseId,
					attributes,
					invitedBy);

			// Fetch the entity so we can set the FK on LeaseTenant
			Invite invite = inviteRepository.findById(inviteResponse.id())
					.orElseThrow(
							() -> new IllegalStateException("Invite not found after creation: " + inviteResponse.id()));

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

		// Capture userId before deleting (tenant may be null if invite not yet
		// accepted)
		UUID tenantUserId = leaseTenant.getTenant() != null
				? leaseTenant.getTenant().getUser().getId()
				: null;

		leaseTenantRepository.delete(leaseTenant);

		if (tenantUserId != null) {
			eventPublisher.publishEvent(new PermissionsChangedEvent(Set.of(tenantUserId)));
		}
		log.info("Removed LeaseTenant {} from lease {}", leaseTenantId, leaseId);
	}

	// ───────────────────────── Invite acceptance ─────────────────────────

	/**
	 * Reacts to a redeemed invite. If the invite targets a LEASE,
	 * finds the corresponding {@link LeaseTenant} slot, creates a {@link Tenant}
	 * profile for the user if they don't have one yet, and links it.
	 */
	@EventListener
	@Transactional
	public void onInviteAccepted(InviteAcceptedEvent event) {
		Invite invite = event.invite();
		if (invite.getTargetType() != TargetType.LEASE) {
			return;
		}

		User claimedUser = event.claimedUser();

		// Get or create tenant profile
		Tenant tenant = tenantRepository.findByUser_Id(claimedUser.getId())
				.orElseGet(() -> {
					log.info("Creating tenant profile for user {} on invite acceptance", claimedUser.getId());
					return tenantRepository.save(Tenant.builder()
							.user(claimedUser)
							.build());
				});

		// Locate slot reserved for this invite
		LeaseTenant leaseTenant = leaseTenantRepository.findByInvite_Id(invite.getId())
				.orElseThrow(
						() -> new IllegalStateException("LeaseTenant slot not found for invite " + invite.getId()));

		// Resolve the role from attributes (with a safe default)
		String roleValue = (String) invite.getAttributes().getOrDefault(ATTR_ROLE, LeaseTenantRole.PRIMARY.name());

		// Link and Persist
		leaseTenant.setTenant(tenant);
		leaseTenant.setRole(LeaseTenantRole.valueOf(roleValue));
		leaseTenantRepository.save(leaseTenant);

		log.info("Linked tenant {} to LeaseTenant {} via invite {}", tenant.getId(), leaseTenant.getId(),
				invite.getId());
	}

	/**
	 * Builds the preview snapshot stored in {@code invite.attributes["preview"]}.
	 * Data is captured at invite-creation time so the accept page can render
	 * without additional repository joins, and so the context is stable even if
	 * the lease or property is later modified.
	 */
	private Map<String, Object> buildPreviewSnapshot(Lease lease) {
		Prop property = lease.getProperty();
		Address address = property.getAddress();
		Unit unit = lease.getUnit();

		Map<String, Object> propertyMap = new LinkedHashMap<>();
		propertyMap.put("legalName", property.getLegalName());
		propertyMap.put("addressLine1", address.getAddressLine1());
		propertyMap.put("addressLine2", address.getAddressLine2());
		propertyMap.put("city", address.getCity());
		propertyMap.put("stateProvinceRegion", address.getStateProvinceRegion());
		propertyMap.put("postalCode", address.getPostalCode());

		Map<String, Object> unitMap = new LinkedHashMap<>();
		unitMap.put("unitNumber", unit.getUnitNumber());
		unitMap.put("unitType", unit.getUnitType() != null ? unit.getUnitType().name() : null);

		Map<String, Object> leaseMap = new LinkedHashMap<>();
		leaseMap.put("startDate", lease.getStartDate() != null ? lease.getStartDate().toString() : null);
		leaseMap.put("endDate", lease.getEndDate() != null ? lease.getEndDate().toString() : null);
		leaseMap.put("rentAmount", lease.getRentAmount());

		Map<String, Object> snapshot = new LinkedHashMap<>();
		snapshot.put("property", propertyMap);
		snapshot.put("unit", unitMap);
		snapshot.put("lease", leaseMap);
		return snapshot;
	}
}
