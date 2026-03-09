package com.akandiah.propmanager.features.activity.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.akandiah.propmanager.features.activity.domain.ActivityEvent;
import com.akandiah.propmanager.features.activity.domain.ActivityEventRepository;
import com.akandiah.propmanager.features.activity.domain.ActivityEventType;
import com.akandiah.propmanager.features.activity.domain.ActivitySubjectType;
import com.akandiah.propmanager.features.invite.domain.Invite;
import com.akandiah.propmanager.features.invite.domain.InviteAcceptedEvent;
import com.akandiah.propmanager.features.invite.domain.TargetType;
import com.akandiah.propmanager.features.lease.domain.LeaseLifecycleEvent;
import com.akandiah.propmanager.features.lease.domain.LeaseLifecycleEventType;
import com.akandiah.propmanager.features.lease.domain.LeaseRepository;
import com.akandiah.propmanager.features.organization.domain.OrganizationCreatedEvent;
import com.akandiah.propmanager.features.unit.domain.UnitRepository;
import com.akandiah.propmanager.security.JwtUserResolver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Persists domain events as activity records after the originating transaction commits. */
@Component
@RequiredArgsConstructor
@Slf4j
public class ActivityEventListener {

	private final ActivityEventRepository repository;
	private final LeaseRepository leaseRepository;
	private final UnitRepository unitRepository;
	private final JwtUserResolver jwtUserResolver;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void onLeaseLifecycle(LeaseLifecycleEvent event) {
		UUID orgId = leaseRepository.findOrgIdById(event.leaseId()).orElse(null);
		if (orgId == null) {
			log.warn("Could not resolve orgId for lease {} — activity event dropped", event.leaseId());
			return;
		}
		var metadataBuilder = new java.util.HashMap<String, Object>();
		if (event.subjectId() != null) {
			metadataBuilder.put("signatoryId", event.subjectId());
		}
		// Enrich with unit/property context for actionable descriptions
		leaseRepository.findUnitIdById(event.leaseId()).ifPresent(unitId -> {
			unitRepository.findByIdWithPropAndOrg(unitId).ifPresent(unit -> {
				metadataBuilder.put("unitNumber", unit.getUnitNumber());
				metadataBuilder.put("propertyName", unit.getProp().getLegalName());
			});
		});
		persist(orgId, mapType(event.type()), ActivitySubjectType.LEASE, event.leaseId(),
				metadataBuilder.isEmpty() ? null : metadataBuilder);
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void onOrgCreated(OrganizationCreatedEvent event) {
		persist(event.organizationId(), ActivityEventType.ORGANIZATION_CREATED,
				ActivitySubjectType.ORGANIZATION, event.organizationId(), null);
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void onInviteAccepted(InviteAcceptedEvent event) {
		UUID orgId = resolveOrgIdForInvite(event.invite());
		if (orgId == null) {
			log.warn("Could not resolve orgId for invite {} — activity event dropped", event.invite().getId());
			return;
		}
		persist(orgId, ActivityEventType.INVITE_ACCEPTED, ActivitySubjectType.INVITE,
				event.invite().getId(), Map.of("email", event.invite().getEmail()));
	}

	private void persist(UUID orgId, ActivityEventType type, ActivitySubjectType subjectType,
			UUID subjectId, Map<String, Object> metadata) {
		UUID actorId = jwtUserResolver.resolveOptionalId().orElse(null);
		var activityEvent = ActivityEvent.builder()
				.orgId(orgId)
				.eventType(type)
				.subjectType(subjectType)
				.subjectId(subjectId)
				.actorId(actorId)
				.metadata(metadata)
				.build();
		repository.save(activityEvent);
	}

	private ActivityEventType mapType(LeaseLifecycleEventType type) {
		return switch (type) {
			case SUBMITTED_FOR_REVIEW -> ActivityEventType.LEASE_SUBMITTED_FOR_REVIEW;
			case TENANT_SIGNED -> ActivityEventType.LEASE_TENANT_SIGNED;
			case TENANT_ALL_SIGNED -> ActivityEventType.LEASE_ALL_TENANTS_SIGNED;
			case ACTIVATED -> ActivityEventType.LEASE_ACTIVATED;
			case EXPIRING_SOON -> ActivityEventType.LEASE_EXPIRING_SOON;
			case EXPIRED -> ActivityEventType.LEASE_EXPIRED;
			case TERMINATED -> ActivityEventType.LEASE_TERMINATED;
			case EVICTED -> ActivityEventType.LEASE_EVICTED;
		};
	}

	private UUID resolveOrgIdForInvite(Invite invite) {
		if (invite.getTargetType() == TargetType.MEMBERSHIP) {
			// For membership invites, targetId IS the org ID
			return invite.getTargetId();
		}
		// For lease invites, resolve org from the lease
		return leaseRepository.findOrgIdById(invite.getTargetId()).orElse(null);
	}
}
