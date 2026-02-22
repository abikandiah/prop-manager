package com.akandiah.propmanager.features.organization.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.features.organization.api.dto.CreateMemberScopeRequest;
import com.akandiah.propmanager.features.organization.api.dto.MemberScopeResponse;
import com.akandiah.propmanager.features.organization.domain.MemberScope;
import com.akandiah.propmanager.features.organization.domain.MemberScopeRepository;
import com.akandiah.propmanager.features.organization.domain.Membership;
import com.akandiah.propmanager.features.organization.domain.MembershipRepository;
import com.akandiah.propmanager.features.prop.domain.PropRepository;
import com.akandiah.propmanager.features.unit.domain.UnitRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberScopeService {

	private final MemberScopeRepository memberScopeRepository;
	private final MembershipRepository membershipRepository;
	private final PropRepository propRepository;
	private final UnitRepository unitRepository;

	public List<MemberScopeResponse> findByMembershipId(UUID membershipId) {
		return memberScopeRepository.findByMembershipId(membershipId).stream()
				.map(s -> MemberScopeResponse.from(s, membershipId))
				.toList();
	}

	@Transactional
	public MemberScopeResponse create(UUID membershipId, CreateMemberScopeRequest request) {
		Membership membership = membershipRepository.findById(membershipId)
				.orElseThrow(() -> new ResourceNotFoundException("Membership", membershipId));

		UUID orgId = membership.getOrganization().getId();
		validateScopeBelongsToOrg(request, orgId);

		MemberScope scope = MemberScope.builder()
				.membership(membership)
				.scopeType(request.scopeType())
				.scopeId(request.scopeId())
				.build();
		scope = memberScopeRepository.save(scope);
		return MemberScopeResponse.from(scope);
	}

	@Transactional
	public void deleteById(UUID membershipId, UUID scopeId) {
		if (!memberScopeRepository.existsByIdAndMembershipId(scopeId, membershipId)) {
			throw new ResourceNotFoundException("MemberScope", scopeId);
		}
		memberScopeRepository.deleteById(scopeId);
	}

	private void validateScopeBelongsToOrg(CreateMemberScopeRequest request, UUID orgId) {
		boolean valid = switch (request.scopeType()) {
			case PROPERTY -> propRepository.existsByIdAndOrganization_Id(request.scopeId(), orgId);
			case UNIT -> unitRepository.existsByIdAndProp_Organization_Id(request.scopeId(), orgId);
		};
		if (!valid) {
			throw new ResourceNotFoundException(request.scopeType().name(), request.scopeId());
		}
	}
}
