package com.akandiah.propmanager.features.lease.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.dto.PageResponse;
import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.common.util.DeleteGuardUtil;
import com.akandiah.propmanager.common.util.OptimisticLockingUtil;
import com.akandiah.propmanager.features.lease.api.dto.CreateLeaseRequest;
import com.akandiah.propmanager.features.lease.api.dto.LeaseResponse;
import com.akandiah.propmanager.features.lease.api.dto.UpdateLeaseRequest;
import com.akandiah.propmanager.features.lease.domain.Lease;
import com.akandiah.propmanager.features.lease.domain.LeaseRepository;
import com.akandiah.propmanager.features.lease.domain.LeaseStatus;
import com.akandiah.propmanager.features.lease.domain.LeaseTemplate;
import com.akandiah.propmanager.features.lease.domain.LeaseTenantRepository;
import com.akandiah.propmanager.features.prop.domain.Prop;
import com.akandiah.propmanager.features.prop.domain.PropRepository;
import com.akandiah.propmanager.features.unit.domain.Unit;
import com.akandiah.propmanager.features.unit.domain.UnitRepository;

@Service
public class LeaseService {

	private final LeaseRepository leaseRepository;
	private final LeaseTemplateService templateService;
	private final UnitRepository unitRepository;
	private final PropRepository propRepository;
	private final LeaseTenantRepository leaseTenantRepository;
	private final LeaseStateMachine stateMachine;
	private final LeaseTemplateRenderer renderer;

	public LeaseService(LeaseRepository leaseRepository,
			LeaseTemplateService templateService,
			UnitRepository unitRepository,
			PropRepository propRepository,
			LeaseTenantRepository leaseTenantRepository,
			LeaseStateMachine stateMachine,
			LeaseTemplateRenderer renderer) {
		this.leaseRepository = leaseRepository;
		this.templateService = templateService;
		this.unitRepository = unitRepository;
		this.propRepository = propRepository;
		this.leaseTenantRepository = leaseTenantRepository;
		this.stateMachine = stateMachine;
		this.renderer = renderer;
	}

	// ───────────────────────── Queries ─────────────────────────

	@Transactional(readOnly = true)
	public List<LeaseResponse> findAll() {
		return leaseRepository.findAll().stream()
				.map(LeaseResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public PageResponse<LeaseResponse> findAll(Pageable pageable) {
		return PageResponse.from(leaseRepository.findAll(pageable)
				.map(LeaseResponse::from));
	}

	@Transactional(readOnly = true)
	public List<LeaseResponse> findByUnitId(UUID unitId) {
		return leaseRepository.findByUnit_IdOrderByStartDateDesc(unitId).stream()
				.map(LeaseResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public PageResponse<LeaseResponse> findByUnitId(UUID unitId, Pageable pageable) {
		return PageResponse.from(leaseRepository.findByUnit_IdOrderByStartDateDesc(unitId, pageable)
				.map(LeaseResponse::from));
	}

	@Transactional(readOnly = true)
	public List<LeaseResponse> findByPropertyId(UUID propertyId) {
		return leaseRepository.findByProperty_IdOrderByStartDateDesc(propertyId).stream()
				.map(LeaseResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public PageResponse<LeaseResponse> findByPropertyId(UUID propertyId, Pageable pageable) {
		return PageResponse.from(leaseRepository.findByProperty_IdOrderByStartDateDesc(propertyId, pageable)
				.map(LeaseResponse::from));
	}

	@Transactional(readOnly = true)
	public LeaseResponse findById(UUID id) {
		Lease lease = getEntity(id);
		return LeaseResponse.from(lease);
	}

	// ───────────────────────── Stamp (create) ─────────────────────────

	/**
	 * Stamps a new lease from a template.
	 * Copies template defaults for any field the caller didn't override,
	 * then renders the template markdown into the executed snapshot.
	 */
	@Transactional
	public LeaseResponse create(CreateLeaseRequest request) {
		LeaseTemplate template = templateService.getEntity(request.leaseTemplateId());
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
				.executedContentMarkdown(renderer.stampMarkdown(template.getTemplateMarkdown(), request, unit, property, template.getTemplateParameters()))
				.build();

		lease = leaseRepository.save(lease);
		return LeaseResponse.from(lease);
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

		lease = leaseRepository.save(lease);
		return LeaseResponse.from(lease);
	}

	// ───────────────────────── Status transitions ─────────────────────────

	/** Owner sends the draft to the tenant for review. */
	@Transactional
	public LeaseResponse submitForReview(UUID id) {
		return stateMachine.submitForReview(id);
	}

	/** Both parties signed — lease becomes active and read-only. */
	@Transactional
	public LeaseResponse activate(UUID id) {
		return stateMachine.activate(id);
	}

	/** Revert a pending-review lease back to draft for further edits. */
	@Transactional
	public LeaseResponse revertToDraft(UUID id) {
		return stateMachine.revertToDraft(id);
	}

	/** Terminate an active lease early. */
	@Transactional
	public LeaseResponse terminate(UUID id) {
		return stateMachine.terminate(id);
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
}
