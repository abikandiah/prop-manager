package com.akandiah.propmanager.features.membership.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.common.permission.PermissionStringValidator;
import com.akandiah.propmanager.common.permission.ResourceType;
import com.akandiah.propmanager.common.util.OptimisticLockingUtil;
import com.akandiah.propmanager.features.asset.domain.AssetRepository;
import com.akandiah.propmanager.features.auth.domain.PermissionsChangedEvent;
import com.akandiah.propmanager.features.membership.api.dto.CreatePolicyAssignmentRequest;
import com.akandiah.propmanager.features.membership.api.dto.PolicyAssignmentResponse;
import com.akandiah.propmanager.features.membership.api.dto.UpdatePolicyAssignmentRequest;
import com.akandiah.propmanager.features.membership.domain.Membership;
import com.akandiah.propmanager.features.membership.domain.PermissionPolicy;
import com.akandiah.propmanager.features.membership.domain.PermissionPolicyRepository;
import com.akandiah.propmanager.features.membership.domain.PolicyAssignment;
import com.akandiah.propmanager.features.membership.domain.PolicyAssignmentRepository;
import com.akandiah.propmanager.features.membership.domain.MembershipRepository;
import com.akandiah.propmanager.features.prop.domain.PropRepository;
import com.akandiah.propmanager.features.unit.domain.UnitRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyAssignmentService {

	private final PolicyAssignmentRepository assignmentRepository;
	private final MembershipRepository membershipRepository;
	private final PermissionPolicyRepository policyRepository;
	private final PropRepository propRepository;
	private final UnitRepository unitRepository;
	private final AssetRepository assetRepository;
	private final ApplicationEventPublisher eventPublisher;

	public List<PolicyAssignmentResponse> findByMembershipId(UUID membershipId) {
		return assignmentRepository.findByMembershipId(membershipId).stream()
				.map(a -> PolicyAssignmentResponse.from(a, membershipId))
				.toList();
	}

	public PolicyAssignmentResponse findById(UUID membershipId, UUID assignmentId) {
		PolicyAssignment assignment = assignmentRepository.findByIdAndMembershipId(assignmentId, membershipId)
				.orElseThrow(() -> new ResourceNotFoundException("PolicyAssignment", assignmentId));
		return PolicyAssignmentResponse.from(assignment, membershipId);
	}

	@Transactional
	public PolicyAssignmentResponse create(UUID membershipId, CreatePolicyAssignmentRequest request) {
		Membership membership = membershipRepository.findById(membershipId)
				.orElseThrow(() -> new ResourceNotFoundException("Membership", membershipId));
		PolicyAssignmentResponse response = doCreate(membership, request);
		if (membership.getUser() != null) {
			eventPublisher.publishEvent(new PermissionsChangedEvent(Set.of(membership.getUser().getId())));
		}
		return response;
	}

	/**
	 * Creates an assignment without publishing a {@link PermissionsChangedEvent}.
	 * Used by {@link MembershipService#inviteMember} which publishes a single event.
	 */
	@Transactional
	PolicyAssignmentResponse createWithoutEvent(UUID membershipId, CreatePolicyAssignmentRequest request) {
		Membership membership = membershipRepository.findById(membershipId)
				.orElseThrow(() -> new ResourceNotFoundException("Membership", membershipId));
		return doCreate(membership, request);
	}

	private PolicyAssignmentResponse doCreate(Membership membership, CreatePolicyAssignmentRequest request) {
		UUID orgId = membership.getOrganization().getId();
		validateResourceBelongsToOrg(request.resourceType(), request.resourceId(), orgId);

		PermissionPolicy policy = null;
		if (request.policyId() != null) {
			policy = policyRepository.findById(request.policyId())
					.orElseThrow(() -> new ResourceNotFoundException("PermissionPolicy", request.policyId()));
			validatePolicyBelongsToOrg(policy, orgId);
		}

		if (request.overrides() != null) {
			PermissionStringValidator.validate(request.overrides());
		}

		PolicyAssignment assignment = PolicyAssignment.builder()
				.id(request.id())
				.membership(membership)
				.resourceType(request.resourceType())
				.resourceId(request.resourceId())
				.policy(policy)
				.overrides(request.overrides())
				.build();
		assignment = assignmentRepository.save(assignment);
		return PolicyAssignmentResponse.from(assignment);
	}

	@Transactional
	public PolicyAssignmentResponse update(UUID membershipId, UUID assignmentId, UpdatePolicyAssignmentRequest request) {
		PolicyAssignment assignment = assignmentRepository.findByIdAndMembershipId(assignmentId, membershipId)
				.orElseThrow(() -> new ResourceNotFoundException("PolicyAssignment", assignmentId));
		OptimisticLockingUtil.requireVersionMatch("PolicyAssignment", assignmentId,
				assignment.getVersion(), request.version());

		if (request.policyId() != null) {
			PermissionPolicy policy = policyRepository.findById(request.policyId())
					.orElseThrow(() -> new ResourceNotFoundException("PermissionPolicy", request.policyId()));
			UUID orgId = assignment.getMembership().getOrganization().getId();
			validatePolicyBelongsToOrg(policy, orgId);
			assignment.setPolicy(policy);
		} else {
			assignment.setPolicy(null);
		}

		if (request.overrides() != null) {
			PermissionStringValidator.validate(request.overrides());
		}
		assignment.setOverrides(request.overrides());

		assignment = assignmentRepository.save(assignment);
		if (assignment.getMembership().getUser() != null) {
			eventPublisher.publishEvent(
					new PermissionsChangedEvent(Set.of(assignment.getMembership().getUser().getId())));
		}
		return PolicyAssignmentResponse.from(assignment, membershipId);
	}

	@Transactional
	public void deleteById(UUID membershipId, UUID assignmentId) {
		PolicyAssignment assignment = assignmentRepository.findByIdAndMembershipId(assignmentId, membershipId)
				.orElseThrow(() -> new ResourceNotFoundException("PolicyAssignment", assignmentId));
		UUID userId = assignment.getMembership().getUser() != null
				? assignment.getMembership().getUser().getId()
				: null;
		assignmentRepository.deleteById(assignmentId);
		if (userId != null) {
			eventPublisher.publishEvent(new PermissionsChangedEvent(Set.of(userId)));
		}
	}

	private void validateResourceBelongsToOrg(ResourceType resourceType, UUID resourceId, UUID orgId) {
		boolean valid = switch (resourceType) {
			case ORG -> resourceId.equals(orgId);
			case PROPERTY -> propRepository.existsByIdAndOrganization_Id(resourceId, orgId);
			case UNIT -> unitRepository.existsByIdAndProp_Organization_Id(resourceId, orgId);
			case ASSET -> assetRepository.existsByIdAndProp_Organization_Id(resourceId, orgId) ||
					assetRepository.existsByIdAndUnit_Prop_Organization_Id(resourceId, orgId);
		};
		if (!valid) {
			throw new ResourceNotFoundException(resourceType.name(), resourceId);
		}
	}

	private void validatePolicyBelongsToOrg(PermissionPolicy policy, UUID orgId) {
		if (policy.getOrg() != null && !policy.getOrg().getId().equals(orgId)) {
			throw new ResourceNotFoundException("PermissionPolicy", policy.getId());
		}
	}
}
