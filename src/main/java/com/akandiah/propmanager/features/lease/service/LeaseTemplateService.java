package com.akandiah.propmanager.features.lease.service;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.common.util.DeleteGuardUtil;
import com.akandiah.propmanager.common.util.OptimisticLockingUtil;
import com.akandiah.propmanager.features.lease.domain.LeaseStatus;
import com.akandiah.propmanager.features.lease.api.dto.CreateLeaseTemplateRequest;
import com.akandiah.propmanager.features.lease.api.dto.LeaseTemplateResponse;
import com.akandiah.propmanager.features.lease.api.dto.UpdateLeaseTemplateRequest;
import com.akandiah.propmanager.features.lease.domain.LeaseRepository;
import com.akandiah.propmanager.features.lease.domain.LeaseTemplate;
import com.akandiah.propmanager.features.lease.domain.LeaseTemplateRepository;
import com.akandiah.propmanager.features.organization.domain.Organization;
import com.akandiah.propmanager.features.organization.domain.OrganizationRepository;

@Service
public class LeaseTemplateService {

	private final LeaseTemplateRepository repository;
	private final LeaseRepository leaseRepository;
	private final OrganizationRepository organizationRepository;

	public LeaseTemplateService(LeaseTemplateRepository repository, LeaseRepository leaseRepository,
			OrganizationRepository organizationRepository) {
		this.repository = repository;
		this.leaseRepository = leaseRepository;
		this.organizationRepository = organizationRepository;
	}

	@Transactional(readOnly = true)
	public List<LeaseTemplateResponse> findAll(UUID orgId) {
		return repository.findByOrg_Id(orgId).stream()
				.map(LeaseTemplateResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<LeaseTemplateResponse> findActive(UUID orgId) {
		return repository.findByOrg_IdAndActiveTrueOrderByNameAsc(orgId).stream()
				.map(LeaseTemplateResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<LeaseTemplateResponse> search(String query, UUID orgId) {
		return repository.findByOrg_IdAndNameContainingIgnoreCaseOrderByNameAsc(orgId, query).stream()
				.map(LeaseTemplateResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public LeaseTemplateResponse findById(UUID id) {
		LeaseTemplate template = repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("LeaseTemplate", id));
		return LeaseTemplateResponse.from(template);
	}

	/** Returns the entity (for use by LeaseService during stamping). */
	@Transactional(readOnly = true)
	public LeaseTemplate getEntity(UUID id) {
		return repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("LeaseTemplate", id));
	}

	@Transactional
	public LeaseTemplateResponse create(CreateLeaseTemplateRequest request) {
		Organization org = organizationRepository.findById(request.orgId())
				.orElseThrow(() -> new ResourceNotFoundException("Organization", request.orgId()));

		LeaseTemplate template = LeaseTemplate.builder()
				.org(org)
				.name(request.name())
				.versionTag(request.versionTag())
				.templateMarkdown(request.templateMarkdown())
				.defaultLateFeeType(request.defaultLateFeeType())
				.defaultLateFeeAmount(request.defaultLateFeeAmount())
				.defaultNoticePeriodDays(request.defaultNoticePeriodDays())
				.templateParameters(request.templateParameters())
				.active(true)
				.build();
		template = repository.save(template);
		return LeaseTemplateResponse.from(template);
	}

	@Transactional
	public LeaseTemplateResponse update(UUID id, UpdateLeaseTemplateRequest request) {
		LeaseTemplate template = repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("LeaseTemplate", id));

		if (template.getOrg() == null || !template.getOrg().getId().equals(request.orgId())) {
			throw new AccessDeniedException("Template does not belong to the specified organization");
		}

		OptimisticLockingUtil.requireVersionMatch("LeaseTemplate", id, template.getVersion(), request.version());

		if (request.name() != null) {
			template.setName(request.name());
		}
		if (request.versionTag() != null) {
			template.setVersionTag(request.versionTag());
		}
		if (request.templateMarkdown() != null) {
			template.setTemplateMarkdown(request.templateMarkdown());
		}
		if (request.defaultLateFeeType() != null) {
			template.setDefaultLateFeeType(request.defaultLateFeeType());
		}
		if (request.defaultLateFeeAmount() != null) {
			template.setDefaultLateFeeAmount(request.defaultLateFeeAmount());
		}
		if (request.defaultNoticePeriodDays() != null) {
			template.setDefaultNoticePeriodDays(request.defaultNoticePeriodDays());
		}
		if (request.active() != null) {
			template.setActive(request.active());
		}
		if (request.templateParameters() != null) {
			template.setTemplateParameters(request.templateParameters());
		}

		template = repository.save(template);
		return LeaseTemplateResponse.from(template);
	}

	/**
	 * Hard-delete. Blocked if any DRAFT lease references this template (it still needs it).
	 * For non-DRAFT leases we clear the template FK (they keep their stamped snapshot)
	 * and then delete the template.
	 */
	@Transactional
	public void deleteById(UUID id, UUID orgId) {
		LeaseTemplate template = repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("LeaseTemplate", id));

		if (template.getOrg() == null || !template.getOrg().getId().equals(orgId)) {
			throw new AccessDeniedException("Template does not belong to the specified organization");
		}

		long draftCount = leaseRepository.countByLeaseTemplate_IdAndStatusIn(id,
				java.util.List.of(LeaseStatus.DRAFT));
		DeleteGuardUtil.requireNoChildren("LeaseTemplate", id, draftCount,
				"DRAFT lease(s)", "Activate or remove those leases first.");
		leaseRepository.clearTemplateReference(id);
		repository.delete(template);
	}
}
