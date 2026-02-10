package com.akandiah.propmanager.features.lease.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.dto.PageResponse;
import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.common.util.OptimisticLockingUtil;
import com.akandiah.propmanager.features.lease.api.dto.CreateLeaseTemplateRequest;
import com.akandiah.propmanager.features.lease.api.dto.LeaseTemplateResponse;
import com.akandiah.propmanager.features.lease.api.dto.UpdateLeaseTemplateRequest;
import com.akandiah.propmanager.features.lease.domain.LeaseTemplate;
import com.akandiah.propmanager.features.lease.domain.LeaseTemplateRepository;

@Service
public class LeaseTemplateService {

	private final LeaseTemplateRepository repository;

	public LeaseTemplateService(LeaseTemplateRepository repository) {
		this.repository = repository;
	}

	@Transactional(readOnly = true)
	public List<LeaseTemplateResponse> findAll() {
		return repository.findAll().stream()
				.map(LeaseTemplateResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public PageResponse<LeaseTemplateResponse> findAll(Pageable pageable) {
		return PageResponse.from(repository.findAll(pageable)
				.map(LeaseTemplateResponse::from));
	}

	@Transactional(readOnly = true)
	public List<LeaseTemplateResponse> findActive() {
		return repository.findByActiveTrueOrderByNameAsc().stream()
				.map(LeaseTemplateResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public PageResponse<LeaseTemplateResponse> findActive(Pageable pageable) {
		return PageResponse.from(repository.findByActiveTrueOrderByNameAsc(pageable)
				.map(LeaseTemplateResponse::from));
	}

	@Transactional(readOnly = true)
	public List<LeaseTemplateResponse> search(String query) {
		return repository.findByNameContainingIgnoreCaseOrderByNameAsc(query).stream()
				.map(LeaseTemplateResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public PageResponse<LeaseTemplateResponse> search(String query, Pageable pageable) {
		return PageResponse.from(repository.findByNameContainingIgnoreCaseOrderByNameAsc(query, pageable)
				.map(LeaseTemplateResponse::from));
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
				.templateParameters(request.templateParameters())
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
		if (request.templateParameters() != null) {
			template.setTemplateParameters(request.templateParameters());
		}

		template = repository.save(template);
		return LeaseTemplateResponse.from(template);
	}

	/**
	 * Soft-delete. Sets active=false instead of removing the record.
	 * Existing leases keep their FK reference intact.
	 */
	@Transactional
	public void deleteById(UUID id) {
		LeaseTemplate template = repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("LeaseTemplate", id));
		template.setActive(false);
		repository.save(template);
	}
}
