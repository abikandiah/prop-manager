package com.akandiah.propmanager.features.lease.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.common.permission.AccessListUtil.LeaseAccessFilter;
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
import com.akandiah.propmanager.features.lease.domain.LeaseTenantRepository;
import com.akandiah.propmanager.features.prop.domain.Prop;
import com.akandiah.propmanager.features.prop.domain.PropRepository;
import com.akandiah.propmanager.features.unit.domain.Unit;
import com.akandiah.propmanager.features.unit.domain.UnitRepository;
import com.akandiah.propmanager.features.user.service.UserService;

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
	private final UserService userService;

	// ───────────────────────── Queries ─────────────────────────

	public List<LeaseResponse> findAll() {
		return leaseRepository.findAll().stream()
				.map(LeaseResponse::from)
				.toList();
	}

	public List<LeaseResponse> findAll(LeaseAccessFilter filter) {
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
		getCurrentUserId().ifPresent(userId -> {
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
	public LeaseResponse create(CreateLeaseRequest request) {
		if (!request.startDate().isBefore(request.endDate())) {
			throw new IllegalArgumentException("Start date must be before end date.");
		}

		LeaseTemplate template = templateService.getEntity(request.leaseTemplateId());
		if (!template.getActive()) {
			throw new IllegalArgumentException("Lease template is not active and cannot be used for new leases.");
		}
		Unit unit = unitRepository.findById(request.unitId())
				.orElseThrow(() -> new ResourceNotFoundException("Unit", request.unitId()));
		Prop property = propRepository.findById(request.propertyId())
				.orElseThrow(() -> new ResourceNotFoundException("Prop", request.propertyId()));

		Lease lease = Lease.builder()
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
	 * Stamps the template markdown into executedContentMarkdown here so participants
	 * can review the rendered lease before signing.
	 */
	@Transactional
	public LeaseResponse submitForReview(UUID id) {
		Lease lease = getEntity(id);
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

	/** Both parties signed — lease becomes active and read-only. */
	@Transactional
	public LeaseResponse activate(UUID id) {
		Lease lease = getEntity(id);
		if (leaseRepository.existsByUnit_IdAndStatusAndIdNot(lease.getUnit().getId(), LeaseStatus.ACTIVE, id)) {
			throw new IllegalStateException("Cannot activate: unit already has an active lease.");
		}
		stateMachine.activate(lease);
		Lease saved = leaseRepository.save(lease);
		eventPublisher.publishEvent(new LeaseLifecycleEvent(saved.getId(), LeaseLifecycleEventType.ACTIVATED));
		return LeaseResponse.from(saved);
	}

	/**
	 * Revert a lease in review back to draft for further edits.
	 * Clears the stamped content so it will be re-stamped on the next submit.
	 */
	@Transactional
	public LeaseResponse revertToDraft(UUID id) {
		Lease lease = getEntity(id);
		stateMachine.revertToDraft(lease);
		lease.setExecutedContentMarkdown(null);
		return LeaseResponse.from(leaseRepository.save(lease));
	}

	/** Terminate an active lease early. */
	@Transactional
	public LeaseResponse terminate(UUID id) {
		Lease lease = getEntity(id);
		stateMachine.terminate(lease);
		return LeaseResponse.from(leaseRepository.save(lease));
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

	private Lease getEntity(UUID id) {
		return leaseRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Lease", id));
	}

	/**
	 * Resolves the current user's internal UUID from the active Security context.
	 * Returns empty if the context has no JWT principal or the user is not registered.
	 */
	private Optional<UUID> getCurrentUserId() {
		try {
			var auth = SecurityContextHolder.getContext().getAuthentication();
			if (auth instanceof JwtAuthenticationToken jwtAuth) {
				return userService.findUserFromJwt(jwtAuth.getToken()).map(u -> u.getId());
			}
		} catch (Exception e) {
			log.warn("Could not resolve current user for tenant isolation check", e);
		}
		return Optional.empty();
	}
}
