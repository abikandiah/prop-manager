package com.akandiah.propmanager.features.lease.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.features.lease.api.dto.CreateLeaseRequest;
import com.akandiah.propmanager.features.lease.api.dto.LeaseResponse;
import com.akandiah.propmanager.features.lease.api.dto.UpdateLeaseRequest;
import com.akandiah.propmanager.features.lease.domain.Lease;
import com.akandiah.propmanager.features.lease.domain.LeaseRepository;
import com.akandiah.propmanager.features.lease.domain.LeaseStatus;
import com.akandiah.propmanager.features.lease.domain.LeaseTemplate;
import com.akandiah.propmanager.features.prop.domain.Prop;
import com.akandiah.propmanager.features.prop.domain.PropRepository;
import com.akandiah.propmanager.features.unit.domain.Unit;
import com.akandiah.propmanager.features.unit.domain.UnitRepository;

import jakarta.persistence.OptimisticLockException;

@Service
public class LeaseService {

	private final LeaseRepository leaseRepository;
	private final LeaseTemplateService templateService;
	private final UnitRepository unitRepository;
	private final PropRepository propRepository;

	public LeaseService(LeaseRepository leaseRepository,
			LeaseTemplateService templateService,
			UnitRepository unitRepository,
			PropRepository propRepository) {
		this.leaseRepository = leaseRepository;
		this.templateService = templateService;
		this.unitRepository = unitRepository;
		this.propRepository = propRepository;
	}

	// ───────────────────────── Queries ─────────────────────────

	@Transactional(readOnly = true)
	public List<LeaseResponse> findAll() {
		return leaseRepository.findAll().stream()
				.map(LeaseResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<LeaseResponse> findByUnitId(UUID unitId) {
		return leaseRepository.findByUnit_IdOrderByStartDateDesc(unitId).stream()
				.map(LeaseResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<LeaseResponse> findByPropertyId(UUID propertyId) {
		return leaseRepository.findByProperty_IdOrderByStartDateDesc(propertyId).stream()
				.map(LeaseResponse::from)
				.toList();
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
				.lateFeeType(coalesce(request.lateFeeType(), template.getDefaultLateFeeType()))
				.lateFeeAmount(coalesce(request.lateFeeAmount(), template.getDefaultLateFeeAmount()))
				.noticePeriodDays(coalesce(request.noticePeriodDays(), template.getDefaultNoticePeriodDays()))
				.additionalMetadata(request.additionalMetadata())
				.executedContentMarkdown(stampMarkdown(template.getTemplateMarkdown(), request, unit, property))
				.build();

		lease = leaseRepository.save(lease);
		return LeaseResponse.from(lease);
	}

	// ───────────────────────── Update (DRAFT only) ─────────────────────────

	@Transactional
	public LeaseResponse update(UUID id, UpdateLeaseRequest request) {
		Lease lease = getEntity(id);
		requireDraft(lease);
		requireVersionMatch(lease, request.version());

		if (request.startDate() != null)
			lease.setStartDate(request.startDate());
		if (request.endDate() != null)
			lease.setEndDate(request.endDate());
		if (request.rentAmount() != null)
			lease.setRentAmount(request.rentAmount());
		if (request.rentDueDay() != null)
			lease.setRentDueDay(request.rentDueDay());
		if (request.securityDepositHeld() != null)
			lease.setSecurityDepositHeld(request.securityDepositHeld());
		if (request.lateFeeType() != null)
			lease.setLateFeeType(request.lateFeeType());
		if (request.lateFeeAmount() != null)
			lease.setLateFeeAmount(request.lateFeeAmount());
		if (request.noticePeriodDays() != null)
			lease.setNoticePeriodDays(request.noticePeriodDays());
		if (request.executedContentMarkdown() != null)
			lease.setExecutedContentMarkdown(request.executedContentMarkdown());
		if (request.additionalMetadata() != null)
			lease.setAdditionalMetadata(request.additionalMetadata());

		lease = leaseRepository.save(lease);
		return LeaseResponse.from(lease);
	}

	// ───────────────────────── Status transitions ─────────────────────────

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

	// ───────────────────────── Delete (DRAFT only) ─────────────────────────

	@Transactional
	public void deleteById(UUID id) {
		Lease lease = getEntity(id);
		requireDraft(lease);
		leaseRepository.delete(lease);
	}

	// ───────────────────────── Helpers ─────────────────────────

	private Lease getEntity(UUID id) {
		return leaseRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Lease", id));
	}

	private void requireDraft(Lease lease) {
		if (lease.getStatus() != LeaseStatus.DRAFT) {
			throw new IllegalStateException(
					"Lease " + lease.getId() + " is " + lease.getStatus() + "; only DRAFT leases can be modified");
		}
	}

	private void requireStatus(Lease lease, LeaseStatus expected, String action) {
		if (lease.getStatus() != expected) {
			throw new IllegalStateException(
					"Cannot " + action + ": lease " + lease.getId()
							+ " is " + lease.getStatus() + " (expected " + expected + ")");
		}
	}

	private void requireVersionMatch(Lease lease, Integer clientVersion) {
		if (!lease.getVersion().equals(clientVersion)) {
			throw new OptimisticLockException(
					"Lease " + lease.getId() + " has been modified by another user. "
							+ "Expected version " + clientVersion
							+ " but current version is " + lease.getVersion());
		}
	}

	/**
	 * Simple placeholder stamping. Replaces {{key}} tokens in the template
	 * markdown with concrete lease values.
	 */
	private String stampMarkdown(String markdown, CreateLeaseRequest req, Unit unit, Prop property) {
		if (markdown == null)
			return null;
		return markdown
				.replace("{{property_name}}", property.getLegalName())
				.replace("{{unit_number}}", unit.getUnitNumber())
				.replace("{{start_date}}", req.startDate().toString())
				.replace("{{end_date}}", req.endDate().toString())
				.replace("{{rent_amount}}", req.rentAmount().toPlainString())
				.replace("{{rent_due_day}}", req.rentDueDay().toString())
				.replace("{{security_deposit}}", req.securityDepositHeld() != null
						? req.securityDepositHeld().toPlainString()
						: "N/A");
	}

	private static <T> T coalesce(T override, T fallback) {
		return override != null ? override : fallback;
	}
}
