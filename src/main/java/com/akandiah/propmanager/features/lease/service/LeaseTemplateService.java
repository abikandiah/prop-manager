package com.akandiah.propmanager.features.lease.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.common.util.OptimisticLockingUtil;
import com.akandiah.propmanager.features.lease.api.dto.CreateLeaseTemplateRequest;
import com.akandiah.propmanager.features.lease.api.dto.LeaseTemplateResponse;
import com.akandiah.propmanager.features.lease.api.dto.UpdateLeaseTemplateRequest;
import com.akandiah.propmanager.features.lease.domain.LeaseRepository;
import com.akandiah.propmanager.features.lease.domain.LeaseTemplate;
import com.akandiah.propmanager.features.lease.domain.LeaseTemplateRepository;

@Service
public class LeaseTemplateService {

	private final LeaseTemplateRepository repository;
	private final LeaseRepository leaseRepository;

	public LeaseTemplateService(LeaseTemplateRepository repository, LeaseRepository leaseRepository) {
		this.repository = repository;
		this.leaseRepository = leaseRepository;
	}

	@Transactional(readOnly = true)
	public List<LeaseTemplateResponse> findAll() {
		return repository.findAll().stream()
				.map(LeaseTemplateResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<LeaseTemplateResponse> findActive() {
		return repository.findByActiveTrueOrderByNameAsc().stream()
				.map(LeaseTemplateResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<LeaseTemplateResponse> search(String query) {
		return repository.findByNameContainingIgnoreCaseOrderByNameAsc(query).stream()
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
		LeaseTemplate template = LeaseTemplate.builder()
				.name(request.name())
				.versionTag(request.versionTag())
				.templateMarkdown(request.templateMarkdown())
				.defaultLateFeeType(request.defaultLateFeeType())
				.defaultLateFeeAmount(request.defaultLateFeeAmount())
				.defaultNoticePeriodDays(request.defaultNoticePeriodDays())
				.build();
		template = repository.save(template);
		return LeaseTemplateResponse.from(template);
	}

	@Transactional
	public LeaseTemplateResponse update(UUID id, UpdateLeaseTemplateRequest request) {
		LeaseTemplate template = repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("LeaseTemplate", id));

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

		template = repository.save(template);
		return LeaseTemplateResponse.from(template);
	}

	/**
	 * Hard-delete. Existing leases keep their denormalized template name/version
	 * and stamped content; only the optional FK is nulled out.
	 */
	@Transactional
	public void deleteById(UUID id) {
		if (!repository.existsById(id)) {
			throw new ResourceNotFoundException("LeaseTemplate", id);
		}
		leaseRepository.clearTemplateReferences(id);
		repository.deleteById(id);
	}
}
