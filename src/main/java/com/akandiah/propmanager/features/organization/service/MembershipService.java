package com.akandiah.propmanager.features.organization.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.common.util.OptimisticLockingUtil;
import com.akandiah.propmanager.features.organization.api.dto.CreateMemberScopeRequest;
import com.akandiah.propmanager.features.organization.api.dto.CreateMembershipRequest;
import com.akandiah.propmanager.features.organization.api.dto.MembershipResponse;
import com.akandiah.propmanager.features.organization.api.dto.UpdateMembershipRequest;
import com.akandiah.propmanager.features.organization.domain.MemberScopeRepository;
import com.akandiah.propmanager.features.organization.domain.Membership;
import com.akandiah.propmanager.features.organization.domain.MembershipRepository;
import com.akandiah.propmanager.features.organization.domain.Organization;
import com.akandiah.propmanager.features.organization.domain.OrganizationRepository;
import com.akandiah.propmanager.features.user.domain.User;
import com.akandiah.propmanager.features.user.domain.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MembershipService {

	private final MembershipRepository membershipRepository;
	private final MemberScopeRepository memberScopeRepository;
	private final MemberScopeService memberScopeService;
	private final OrganizationRepository organizationRepository;
	private final UserRepository userRepository;

	public List<MembershipResponse> findByOrganizationId(UUID organizationId) {
		return membershipRepository.findByOrganizationIdWithUserAndOrg(organizationId).stream()
				.map(MembershipResponse::from)
				.toList();
	}

	public List<MembershipResponse> findByUserId(UUID userId) {
		return membershipRepository.findByUserIdWithUserAndOrg(userId).stream()
				.map(MembershipResponse::from)
				.toList();
	}

	public MembershipResponse findById(UUID id) {
		Membership m = membershipRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Membership", id));
		return MembershipResponse.from(m);
	}

	@Transactional
	public MembershipResponse create(UUID organizationId, CreateMembershipRequest request) {
		Organization org = organizationRepository.findById(organizationId)
				.orElseThrow(() -> new ResourceNotFoundException("Organization", organizationId));
		User user = userRepository.findById(request.userId())
				.orElseThrow(() -> new ResourceNotFoundException("User", request.userId()));

		// Duplicate (user, org) is rejected by uk_memberships_user_org → DataIntegrityViolationException → 409
		Membership m = Membership.builder()
				.user(user)
				.organization(org)
				.build();
		m = membershipRepository.save(m);
		return MembershipResponse.from(m);
	}

	/**
	 * Creates a membership and immediately grants an initial scope. Pass {@code null} for
	 * {@code initialScope} when no scope is needed (e.g. tenant onboarding — access comes from LeaseTenant).
	 */
	@Transactional
	public MembershipResponse createWithInitialScope(UUID organizationId, CreateMembershipRequest request,
			CreateMemberScopeRequest initialScope) {
		MembershipResponse membership = create(organizationId, request);
		if (initialScope != null) {
			memberScopeService.create(membership.id(), initialScope);
		}
		return membership;
	}

	@Transactional
	public MembershipResponse update(UUID id, UpdateMembershipRequest request) {
		Membership m = membershipRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Membership", id));
		OptimisticLockingUtil.requireVersionMatch("Membership", id, m.getVersion(), request.version());
		m = membershipRepository.save(m);
		return MembershipResponse.from(m);
	}

	/**
	 * Updates a membership, verifying it belongs to the given organization.
	 */
	@Transactional
	public MembershipResponse update(UUID organizationId, UUID membershipId, UpdateMembershipRequest request) {
		Membership m = membershipRepository.findByIdAndOrganizationId(membershipId, organizationId)
				.orElseThrow(() -> new ResourceNotFoundException("Membership", membershipId));
		OptimisticLockingUtil.requireVersionMatch("Membership", membershipId, m.getVersion(), request.version());
		m = membershipRepository.save(m);
		return MembershipResponse.from(m);
	}

	@Transactional
	public void deleteById(UUID id) {
		Membership m = membershipRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Membership", id));
		memberScopeRepository.deleteByMembershipId(id);
		membershipRepository.delete(m);
	}

	/**
	 * Deletes a membership, verifying it belongs to the given organization.
	 */
	@Transactional
	public void deleteById(UUID organizationId, UUID membershipId) {
		Membership m = membershipRepository.findByIdAndOrganizationId(membershipId, organizationId)
				.orElseThrow(() -> new ResourceNotFoundException("Membership", membershipId));
		memberScopeRepository.deleteByMembershipId(membershipId);
		membershipRepository.delete(m);
	}
}
