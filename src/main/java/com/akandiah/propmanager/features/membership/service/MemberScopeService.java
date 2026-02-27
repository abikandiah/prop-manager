package com.akandiah.propmanager.features.membership.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.common.permission.PermissionStringValidator;
import com.akandiah.propmanager.common.util.OptimisticLockingUtil;
import com.akandiah.propmanager.features.membership.api.dto.CreateMemberScopeRequest;
import com.akandiah.propmanager.features.membership.api.dto.MemberScopeResponse;
import com.akandiah.propmanager.features.membership.api.dto.UpdateMemberScopeRequest;
import com.akandiah.propmanager.features.auth.domain.PermissionsChangedEvent;
import com.akandiah.propmanager.features.membership.domain.MemberScope;
import com.akandiah.propmanager.features.membership.domain.MemberScopeRepository;
import com.akandiah.propmanager.features.membership.domain.Membership;
import com.akandiah.propmanager.features.membership.domain.MembershipRepository;
import com.akandiah.propmanager.features.asset.domain.AssetRepository;
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
	private final AssetRepository assetRepository;
	private final ApplicationEventPublisher eventPublisher;

	public List<MemberScopeResponse> findByMembershipId(UUID membershipId) {
		return memberScopeRepository.findByMembershipId(membershipId).stream()
				.map(s -> MemberScopeResponse.from(s, membershipId))
				.toList();
	}

	public MemberScopeResponse findById(UUID membershipId, UUID scopeId) {
		MemberScope scope = memberScopeRepository.findByIdAndMembershipId(scopeId, membershipId)
				.orElseThrow(() -> new ResourceNotFoundException("MemberScope", scopeId));
		return MemberScopeResponse.from(scope, membershipId);
	}

	@Transactional
	public MemberScopeResponse create(UUID membershipId, CreateMemberScopeRequest request) {
		Membership membership = membershipRepository.findById(membershipId)
				.orElseThrow(() -> new ResourceNotFoundException("Membership", membershipId));
		MemberScopeResponse response = doCreate(membership, request);
		if (membership.getUser() != null) {
			eventPublisher.publishEvent(new PermissionsChangedEvent(Set.of(membership.getUser().getId())));
		}
		return response;
	}

	/**
	 * Creates a scope without publishing a {@link PermissionsChangedEvent}.
	 * Used by {@link MembershipService#createWithInitialScope} which publishes
	 * a single event after both the membership and scope are created.
	 */
	@Transactional
	MemberScopeResponse createWithoutEvent(UUID membershipId, CreateMemberScopeRequest request) {
		Membership membership = membershipRepository.findById(membershipId)
				.orElseThrow(() -> new ResourceNotFoundException("Membership", membershipId));
		return doCreate(membership, request);
	}

	private MemberScopeResponse doCreate(Membership membership, CreateMemberScopeRequest request) {
		UUID orgId = membership.getOrganization().getId();
		validateScopeBelongsToOrg(request, orgId);

		Map<String, String> permissions = request.permissions() != null
				? request.permissions()
				: Collections.emptyMap();
		PermissionStringValidator.validate(permissions);

		MemberScope scope = MemberScope.builder()
				.id(request.id())
				.membership(membership)
				.scopeType(request.scopeType())
				.scopeId(request.scopeId())
				.permissions(permissions)
				.build();
		scope = memberScopeRepository.save(scope);
		return MemberScopeResponse.from(scope);
	}

	@Transactional
	public MemberScopeResponse update(UUID membershipId, UUID scopeId, UpdateMemberScopeRequest request) {
		MemberScope scope = memberScopeRepository.findByIdAndMembershipId(scopeId, membershipId)
				.orElseThrow(() -> new ResourceNotFoundException("MemberScope", scopeId));
		OptimisticLockingUtil.requireVersionMatch("MemberScope", scopeId, scope.getVersion(), request.version());

		Map<String, String> permissions = request.permissions() != null
				? request.permissions()
				: Collections.emptyMap();
		PermissionStringValidator.validate(permissions);

		scope.setPermissions(permissions);
		scope = memberScopeRepository.save(scope);
		if (scope.getMembership().getUser() != null) {
			eventPublisher.publishEvent(
					new PermissionsChangedEvent(Set.of(scope.getMembership().getUser().getId())));
		}
		return MemberScopeResponse.from(scope, membershipId);
	}

	@Transactional
	public void deleteById(UUID membershipId, UUID scopeId) {
		MemberScope scope = memberScopeRepository.findByIdAndMembershipId(scopeId, membershipId)
				.orElseThrow(() -> new ResourceNotFoundException("MemberScope", scopeId));
		UUID userId = scope.getMembership().getUser() != null
				? scope.getMembership().getUser().getId()
				: null;
		memberScopeRepository.deleteById(scopeId);
		if (userId != null) {
			eventPublisher.publishEvent(new PermissionsChangedEvent(Set.of(userId)));
		}
	}

	private void validateScopeBelongsToOrg(CreateMemberScopeRequest request, UUID orgId) {
		boolean valid = switch (request.scopeType()) {
			case ORG -> request.scopeId().equals(orgId);
			case PROPERTY -> propRepository.existsByIdAndOrganization_Id(request.scopeId(), orgId);
			case UNIT -> unitRepository.existsByIdAndProp_Organization_Id(request.scopeId(), orgId);
			case ASSET -> assetRepository.existsByIdAndProp_Organization_Id(request.scopeId(), orgId) ||
					assetRepository.existsByIdAndUnit_Prop_Organization_Id(request.scopeId(), orgId);
		};
		if (!valid) {
			throw new ResourceNotFoundException(request.scopeType().name(), request.scopeId());
		}
	}
}
