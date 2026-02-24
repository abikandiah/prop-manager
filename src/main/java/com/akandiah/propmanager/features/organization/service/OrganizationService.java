package com.akandiah.propmanager.features.organization.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.common.util.DeleteGuardUtil;
import com.akandiah.propmanager.common.util.OptimisticLockingUtil;
import com.akandiah.propmanager.features.membership.api.dto.CreateMemberScopeRequest;
import com.akandiah.propmanager.features.membership.api.dto.CreateMembershipRequest;
import com.akandiah.propmanager.features.organization.api.dto.CreateOrganizationRequest;
import com.akandiah.propmanager.features.membership.api.dto.MembershipResponse;
import com.akandiah.propmanager.features.organization.api.dto.OrganizationResponse;
import com.akandiah.propmanager.features.organization.api.dto.UpdateOrganizationRequest;
import com.akandiah.propmanager.common.permission.ResourceType;
import com.akandiah.propmanager.features.membership.domain.MembershipRepository;
import com.akandiah.propmanager.features.membership.service.MemberScopeService;
import com.akandiah.propmanager.features.membership.service.MembershipService;
import com.akandiah.propmanager.features.organization.domain.Organization;
import com.akandiah.propmanager.features.organization.domain.OrganizationRepository;
import com.akandiah.propmanager.features.prop.domain.PropRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizationService {

	private final OrganizationRepository repository;
	private final MembershipRepository membershipRepository;
	private final MembershipService membershipService;
	private final MemberScopeService memberScopeService;
	private final PropRepository propRepository;

	/**
	 * Returns only the organizations the given user is a member of (data-isolated).
	 */
	public List<OrganizationResponse> findAll(UUID currentUserId) {
		return membershipRepository.findByUserIdWithOrganization(currentUserId).stream()
				.map(m -> OrganizationResponse.from(m.getOrganization()))
				.toList();
	}

	/** Returns all organizations — for system-level ADMIN use only. */
	public List<OrganizationResponse> findAllForAdmin() {
		return repository.findAll().stream()
				.map(OrganizationResponse::from)
				.toList();
	}

	public OrganizationResponse findById(UUID id) {
		Organization org = repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Organization", id));
		return OrganizationResponse.from(org);
	}

	/**
	 * Creates an organization and auto-enrolls the creator as an org-wide admin
	 * (full CRUD on all 4 permission domains).
	 */
	@Transactional
	public OrganizationResponse create(CreateOrganizationRequest request, UUID creatorUserId) {
		Organization org = Organization.builder()
				.name(request.name())
				.taxId(request.taxId())
				.settings(request.settings())
				.build();
		org = repository.save(org);

		// Auto-enroll creator with full ORG-scoped permissions
		MembershipResponse membership = membershipService.create(org.getId(),
				new CreateMembershipRequest(creatorUserId));
		memberScopeService.create(membership.id(), new CreateMemberScopeRequest(
				ResourceType.ORG,
				org.getId(),
				Map.of("l", "rcud", "m", "rcud", "f", "rcud", "t", "rcud"),
				null));

		return OrganizationResponse.from(org);
	}

	@Transactional
	public OrganizationResponse update(UUID id, UpdateOrganizationRequest request) {
		Organization org = repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Organization", id));
		OptimisticLockingUtil.requireVersionMatch("Organization", id, org.getVersion(), request.version());

		if (request.name() != null) {
			org.setName(request.name());
		}
		if (request.taxId() != null) {
			org.setTaxId(request.taxId());
		}
		if (request.settings() != null) {
			org.setSettings(request.settings());
		}
		org = repository.save(org);
		return OrganizationResponse.from(org);
	}

	@Transactional
	public void deleteById(UUID id) {
		if (!repository.existsById(id)) {
			throw new ResourceNotFoundException("Organization", id);
		}

		long membershipCount = membershipRepository.countByOrganizationId(id);
		DeleteGuardUtil.requireNoChildren("Organization", id, membershipCount, "membership(s)",
				"Remove members first.");
		long propCount = propRepository.countByOrganization_Id(id);
		DeleteGuardUtil.requireNoChildren("Organization", id, propCount, "prop(s)", "Move or delete those first.");

		repository.deleteById(id);
	}
}
