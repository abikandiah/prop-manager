package com.akandiah.propmanager.features.lease.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.features.auth.domain.PermissionsChangedEvent;
import com.akandiah.propmanager.common.permission.AccessListUtil.ScopedAccessFilter;
import com.akandiah.propmanager.common.util.DeleteGuardUtil;
import com.akandiah.propmanager.common.util.OptimisticLockingUtil;
import com.akandiah.propmanager.features.lease.api.dto.CreateLeaseRequest;
import com.akandiah.propmanager.features.lease.api.dto.LeaseResponse;
import com.akandiah.propmanager.features.lease.api.dto.UpdateLeaseRequest;
import com.akandiah.propmanager.features.lease.domain.Lease;
import com.akandiah.propmanager.features.lease.domain.LeaseLifecycleEvent;
import com.akandiah.propmanager.features.lease.domain.LeaseLifecycleEventType;
import com.akandiah.propmanager.features.lease.domain.LeaseRepository;
import com.akandiah.propmanager.features.lease.domain.LeaseStatus;
import com.akandiah.propmanager.features.lease.domain.LeaseTemplate;
import com.akandiah.propmanager.features.lease.domain.LeaseTenant;
import com.akandiah.propmanager.features.lease.domain.LeaseTenantRepository;
import com.akandiah.propmanager.features.lease.domain.LeaseTenantStatus;
import com.akandiah.propmanager.features.prop.domain.Prop;
import com.akandiah.propmanager.features.prop.domain.PropRepository;
import com.akandiah.propmanager.features.unit.domain.Unit;
import com.akandiah.propmanager.features.unit.domain.UnitRepository;
import com.akandiah.propmanager.features.unit.domain.UnitStatus;
import com.akandiah.propmanager.security.JwtUserResolver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class LeaseService {

	private final LeaseRepository leaseRepository;
	private final LeaseTemplateService templateService;
	private final UnitRepository unitRepository;
	private final PropRepository propRepository;
	private final LeaseTenantRepository leaseTenantRepository;
	private final LeaseStateMachine stateMachine;
	private final LeaseTemplateRenderer renderer;
	private final ApplicationEventPublisher eventPublisher;
	private final JwtUserResolver jwtUserResolver;

	// ───────────────────────── Queries ─────────────────────────

	public List<LeaseResponse> findAll() {
		return leaseRepository.findAll().stream()
				.map(LeaseResponse::from)
				.toList();
	}

	public List<LeaseResponse> findAll(ScopedAccessFilter filter) {
		if (filter.isEmpty()) return List.of();
		return leaseRepository.findByAccessFilter(filter.orgIds(), filter.propIds(), filter.unitIds()).stream()
				.map(LeaseResponse::from)
				.toList();
	}

	public List<LeaseResponse> findByUnitId(UUID unitId) {
		return leaseRepository.findByUnit_IdOrderByStartDateDesc(unitId).stream()
				.map(LeaseResponse::from)
				.toList();
	}

	public List<LeaseResponse> findByPropertyId(UUID propertyId) {
		return leaseRepository.findByProperty_IdOrderByStartDateDesc(propertyId).stream()
				.map(LeaseResponse::from)
				.toList();
	}

	public LeaseResponse findById(UUID id) {
		Lease lease = getEntity(id);
		// Tenant isolation: if the caller is a tenant on this unit, they may only see their own lease,
		// not co-tenants' leases on the same unit.
		jwtUserResolver.resolveOptionalId().ifPresent(userId -> {
			if (leaseTenantRepository.existsByLease_Unit_IdAndTenant_User_Id(lease.getUnit().getId(), userId)) {
				if (!leaseTenantRepository.existsByLease_IdAndTenant_User_Id(id, userId)) {
					throw new AccessDeniedException("Tenants can only access their own lease");
				}
			}
		});
		return LeaseResponse.from(lease);
	}

	// ───────────────────────── Stamp (create) ─────────────────────────

	/**
	 * Stamps a new DRAFT lease from a template.
	 * Copies template defaults for any field the caller didn't override.
	 * Template markdown is rendered into executedContentMarkdown on activate, not here.
	 */
	@Transactional
	public LeaseResponse create(CreateLeaseRequest request, UUID orgId) {
		if (!request.startDate().isBefore(request.endDate())) {
			throw new IllegalArgumentException("Start date must be before end date.");
		}

		LeaseTemplate template = templateService.getEntity(request.leaseTemplateId());
		if (template.getOrg() == null || !template.getOrg().getId().equals(orgId)) {
			throw new AccessDeniedException("Lease template does not belong to the specified organization");
		}
		if (!template.getActive()) {
			throw new IllegalArgumentException("Lease template is not active and cannot be used for new leases.");
		}
		Unit unit = unitRepository.findById(request.unitId())
				.orElseThrow(() -> new ResourceNotFoundException("Unit", request.unitId()));
		Prop property = propRepository.findById(request.propertyId())
				.orElseThrow(() -> new ResourceNotFoundException("Prop", request.propertyId()));

		Lease lease = Lease.builder()
				.id(request.id())
				.leaseTemplate(template)
				.leaseTemplateName(template.getName())
				.leaseTemplateVersionTag(template.getVersionTag())
				.unit(unit)
				.property(property)
				.status(LeaseStatus.DRAFT)
				.startDate(request.startDate())
				.endDate(request.endDate())
				.rentAmount(request.rentAmount())
				.rentDueDay(request.rentDueDay())
				.securityDepositHeld(request.securityDepositHeld())
				.lateFeeType(LeaseTemplateRenderer.coalesce(request.lateFeeType(), template.getDefaultLateFeeType()))
				.lateFeeAmount(LeaseTemplateRenderer.coalesce(request.lateFeeAmount(), template.getDefaultLateFeeAmount()))
				.noticePeriodDays(LeaseTemplateRenderer.coalesce(request.noticePeriodDays(), template.getDefaultNoticePeriodDays()))
				.additionalMetadata(request.additionalMetadata())
				.templateParameters(request.templateParameters())
				.build();

		return LeaseResponse.from(leaseRepository.save(lease));
	}

	// ───────────────────────── Update (DRAFT only) ─────────────────────────

	@Transactional
	public LeaseResponse update(UUID id, UpdateLeaseRequest request) {
		Lease lease = getEntity(id);
		stateMachine.requireDraft(lease);
		OptimisticLockingUtil.requireVersionMatch("Lease", id, lease.getVersion(), request.version());

		if (request.startDate() != null) {
			lease.setStartDate(request.startDate());
		}
		if (request.endDate() != null) {
			lease.setEndDate(request.endDate());
		}
		if (!lease.getStartDate().isBefore(lease.getEndDate())) {
			throw new IllegalArgumentException("Start date must be before end date.");
		}
		if (request.rentAmount() != null) {
			lease.setRentAmount(request.rentAmount());
		}
		if (request.rentDueDay() != null) {
			lease.setRentDueDay(request.rentDueDay());
		}
		if (request.securityDepositHeld() != null) {
			lease.setSecurityDepositHeld(request.securityDepositHeld());
		}
		if (request.lateFeeType() != null) {
			lease.setLateFeeType(request.lateFeeType());
		}
		if (request.lateFeeAmount() != null) {
			lease.setLateFeeAmount(request.lateFeeAmount());
		}
		if (request.noticePeriodDays() != null) {
			lease.setNoticePeriodDays(request.noticePeriodDays());
		}
		if (request.executedContentMarkdown() != null) {
			lease.setExecutedContentMarkdown(request.executedContentMarkdown());
		}
		if (request.additionalMetadata() != null) {
			lease.setAdditionalMetadata(request.additionalMetadata());
		}
		if (request.templateParameters() != null) {
			lease.setTemplateParameters(request.templateParameters());
		}

		return LeaseResponse.from(leaseRepository.save(lease));
	}

	// ───────────────────────── Status transitions ─────────────────────────

	/**
	 * Owner sends the draft to the tenant for review.
	 * Guard: at least one tenant must be invited.
	 * Stamps the template markdown into executedContentMarkdown so participants
	 * can review the rendered lease before signing.
	 */
	@Transactional
	public LeaseResponse submitForReview(UUID id) {
		Lease lease = getEntity(id);

		long tenantCount = leaseTenantRepository.countByLease_Id(id);
		if (tenantCount == 0) {
			throw new IllegalStateException("Cannot submit for review: invite at least one tenant first.");
		}

		stateMachine.submitForReview(lease);
		if (lease.getLeaseTemplate() != null) {
			LeaseTemplate t = lease.getLeaseTemplate();
			lease.setExecutedContentMarkdown(renderer.stampMarkdownFromLease(
					t.getTemplateMarkdown(), lease, lease.getUnit(), lease.getProperty(), t.getTemplateParameters()));
			lease.setLeaseTemplateName(t.getName());
			lease.setLeaseTemplateVersionTag(t.getVersionTag());
		}
		Lease saved = leaseRepository.save(lease);
		eventPublisher.publishEvent(new LeaseLifecycleEvent(saved.getId(), LeaseLifecycleEventType.SUBMITTED_FOR_REVIEW));
		return LeaseResponse.from(saved);
	}

	/** All tenants signed — manager activates lease. Requires SIGNED status and all signatures valid. */
	@Transactional
	public LeaseResponse activate(UUID id) {
		Lease lease = getEntity(id);

		if (leaseRepository.existsByUnit_IdAndStatusAndIdNot(lease.getUnit().getId(), LeaseStatus.ACTIVE, id)) {
			throw new IllegalStateException("Cannot activate: unit already has an active lease.");
		}

		List<LeaseTenant> tenants = leaseTenantRepository.findByLease_Id(id);
		if (tenants.isEmpty()) {
			throw new IllegalStateException("Cannot activate: no tenants on this lease.");
		}
		for (LeaseTenant lt : tenants) {
			if (lt.getStatus() != LeaseTenantStatus.SIGNED) {
				throw new IllegalStateException(
						"Cannot activate: " + lt.getInvite().getEmail() + " has not signed.");
			}
			if (!lease.getVersion().equals(lt.getAcceptedLeaseVersion())) {
				throw new IllegalStateException(
						"Cannot activate: " + lt.getInvite().getEmail()
						+ " signed a previous version. They must re-sign.");
			}
		}

		stateMachine.activate(lease);   // SIGNED → ACTIVE
		updateUnitStatus(lease.getUnit(), UnitStatus.OCCUPIED);
		Lease saved = leaseRepository.save(lease);
		eventPublisher.publishEvent(new LeaseLifecycleEvent(saved.getId(), LeaseLifecycleEventType.ACTIVATED));
		publishPermissionsChangedForLeaseTenants(id);
		return LeaseResponse.from(saved);
	}

	/**
	 * Revert a lease in REVIEW or SIGNED back to DRAFT for further edits.
	 * Clears the stamped content so it will be re-stamped on the next submit.
	 */
	@Transactional
	public LeaseResponse revertToDraft(UUID id) {
		Lease lease = getEntity(id);
		stateMachine.revertToDraft(lease);
		lease.setExecutedContentMarkdown(null);
		return LeaseResponse.from(leaseRepository.save(lease));
	}

	/** Terminate an active lease early. Updates unit to VACANT and publishes event. */
	@Transactional
	public LeaseResponse terminate(UUID id) {
		Lease lease = getEntity(id);
		stateMachine.terminate(lease);
		updateUnitStatus(lease.getUnit(), UnitStatus.VACANT);
		Lease saved = leaseRepository.save(lease);
		publishPermissionsChangedForLeaseTenants(id);
		eventPublisher.publishEvent(new LeaseLifecycleEvent(id, LeaseLifecycleEventType.TERMINATED));
		return LeaseResponse.from(saved);
	}

	/** Evict tenants from an active lease. Updates unit to VACANT and publishes event. */
	@Transactional
	public LeaseResponse evict(UUID id) {
		Lease lease = getEntity(id);
		stateMachine.evict(lease);
		updateUnitStatus(lease.getUnit(), UnitStatus.VACANT);
		Lease saved = leaseRepository.save(lease);
		publishPermissionsChangedForLeaseTenants(id);
		eventPublisher.publishEvent(new LeaseLifecycleEvent(id, LeaseLifecycleEventType.EVICTED));
		return LeaseResponse.from(saved);
	}

	/** Auto-advances a REVIEW lease to SIGNED after all tenants have signed. Package-private for LeaseTenantService. */
	@Transactional
	void advanceToSigned(UUID id) {
		Lease lease = getEntity(id);
		stateMachine.advanceToSigned(lease);
		leaseRepository.save(lease);
	}

	// ───────────────────────── Delete (DRAFT only) ─────────────────────────

	@Transactional
	public void deleteById(UUID id) {
		Lease lease = getEntity(id);
		stateMachine.requireDraft(lease);
		DeleteGuardUtil.requireNoChildren("Lease", id, leaseTenantRepository.countByLease_Id(id), "tenant assignment(s)", "Remove those first.");
		leaseRepository.delete(lease);
	}

	// ───────────────────────── Helpers ─────────────────────────

	private void updateUnitStatus(Unit unit, UnitStatus newStatus) {
		unit.setStatus(newStatus);
		unitRepository.save(unit);
	}

	void publishPermissionsChangedForLeaseTenants(UUID leaseId) {
		Set<UUID> userIds = leaseTenantRepository.findByLease_IdWithTenantUser(leaseId).stream()
				.map(lt -> lt.getTenant().getUser().getId())
				.collect(Collectors.toSet());
		if (!userIds.isEmpty()) {
			eventPublisher.publishEvent(new PermissionsChangedEvent(userIds));
		}
	}

	Lease getEntity(UUID id) {
		return leaseRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Lease", id));
	}

}
