package com.akandiah.propmanager.features.membership.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.common.permission.Actions;
import com.akandiah.propmanager.common.permission.PermissionStringValidator;
import com.akandiah.propmanager.common.permission.ResourceType;
import com.akandiah.propmanager.common.util.DeleteGuardUtil;
import com.akandiah.propmanager.common.util.OptimisticLockingUtil;
import com.akandiah.propmanager.common.util.SecurityUtils;
import com.akandiah.propmanager.features.auth.domain.PermissionsChangedEvent;
import com.akandiah.propmanager.features.membership.api.dto.CreatePermissionPolicyRequest;
import com.akandiah.propmanager.features.membership.api.dto.PermissionPolicyResponse;
import com.akandiah.propmanager.features.membership.api.dto.UpdatePermissionPolicyRequest;
import com.akandiah.propmanager.features.membership.domain.Membership;
import com.akandiah.propmanager.features.membership.domain.PermissionPolicy;
import com.akandiah.propmanager.features.membership.domain.PermissionPolicyRepository;
import com.akandiah.propmanager.features.membership.domain.PolicyAssignmentRepository;
import com.akandiah.propmanager.features.organization.domain.Organization;
import com.akandiah.propmanager.features.organization.domain.OrganizationRepository;
import com.akandiah.propmanager.security.OrgGuard;
import com.akandiah.propmanager.security.PermissionGuard;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PermissionPolicyService {

	private final PermissionPolicyRepository repository;
	private final OrganizationRepository organizationRepository;
	private final PolicyAssignmentRepository assignmentRepository;
	private final OrgGuard orgGuard;
	private final PermissionGuard permissionGuard;
	private final ApplicationEventPublisher eventPublisher;

	public List<PermissionPolicyResponse> listByOrg(UUID orgId) {
		return repository.findByOrgIsNullOrOrg_IdOrderByNameAsc(orgId).stream()
				.map(PermissionPolicyResponse::from)
				.toList();
	}

	public PermissionPolicyResponse findById(UUID id) {
		PermissionPolicy policy = repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("PermissionPolicy", id));

		if (policy.getOrg() != null) {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			if (!orgGuard.isMember(policy.getOrg().getId(), auth)) {
				throw new AccessDeniedException("Access denied to this organization's policies");
			}
		}

		return PermissionPolicyResponse.from(policy);
	}

	@Transactional
	public PermissionPolicyResponse create(CreatePermissionPolicyRequest request, UUID orgId) {
		if (orgId == null && !SecurityUtils.isGlobalAdmin()) {
			throw new AccessDeniedException("Only system administrators can create system policies");
		}

		PermissionStringValidator.validate(request.permissions());

		Organization org = null;
		if (orgId != null) {
			org = organizationRepository.findById(orgId)
					.orElseThrow(() -> new ResourceNotFoundException("Organization", orgId));
		}

		PermissionPolicy policy = PermissionPolicy.builder()
				.id(request.id())
				.org(org)
				.name(request.name())
				.permissions(request.permissions())
				.build();
		policy = repository.save(policy);
		return PermissionPolicyResponse.from(policy);
	}

	@Transactional
	public PermissionPolicyResponse update(UUID id, UpdatePermissionPolicyRequest request, UUID orgId) {
		PermissionPolicy policy = repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("PermissionPolicy", id));

		if (policy.getOrg() != null && !policy.getOrg().getId().equals(orgId)) {
			throw new AccessDeniedException("Policy does not belong to the specified organization");
		}

		requireUpdateAccess(policy, orgId);
		OptimisticLockingUtil.requireVersionMatch("PermissionPolicy", id, policy.getVersion(), request.version());

		boolean permissionsChanged = false;
		if (request.name() != null) {
			policy.setName(request.name());
		}
		if (request.permissions() != null) {
			PermissionStringValidator.validate(request.permissions());
			policy.setPermissions(request.permissions());
			permissionsChanged = true;
		}

		policy = repository.save(policy);

		if (permissionsChanged) {
			evictLinkedMemberships(id);
		}

		return PermissionPolicyResponse.from(policy);
	}

	@Transactional
	public void deleteById(UUID id, UUID orgId) {
		PermissionPolicy policy = repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("PermissionPolicy", id));

		if (policy.getOrg() != null && !policy.getOrg().getId().equals(orgId)) {
			throw new AccessDeniedException("Policy does not belong to the specified organization");
		}

		requireDeleteAccess(policy, orgId);

		long assignmentCount = assignmentRepository.countByPolicyId(id);
		DeleteGuardUtil.requireNoChildren("PermissionPolicy", id, assignmentCount, "active assignment(s)",
				"reassign or remove those first");

		evictLinkedMemberships(id);
		repository.delete(policy);
	}

	// --- helpers ---

	private void requireUpdateAccess(PermissionPolicy policy, UUID orgId) {
		if (policy.getOrg() == null) {
			if (!SecurityUtils.isGlobalAdmin()) {
				throw new AccessDeniedException("Only system administrators can modify system policies");
			}
		} else {
			if (!SecurityUtils.isGlobalAdmin() &&
					!permissionGuard.hasAccess(Actions.UPDATE, "o", ResourceType.ORG, orgId, orgId)) {
				throw new AccessDeniedException("Insufficient permissions to modify this organization's policies");
			}
		}
	}

	private void requireDeleteAccess(PermissionPolicy policy, UUID orgId) {
		if (policy.getOrg() == null) {
			if (!SecurityUtils.isGlobalAdmin()) {
				throw new AccessDeniedException("Only system administrators can delete system policies");
			}
		} else {
			if (!SecurityUtils.isGlobalAdmin() &&
					!permissionGuard.hasAccess(Actions.DELETE, "o", ResourceType.ORG, orgId, orgId)) {
				throw new AccessDeniedException("Insufficient permissions to delete this organization's policies");
			}
		}
	}

	/**
	 * Evicts the permissions cache for all users whose assignments reference this policy.
	 */
	private void evictLinkedMemberships(UUID policyId) {
		List<Membership> linked = assignmentRepository.findByPolicyId(policyId)
				.stream()
				.map(a -> a.getMembership())
				.filter(m -> m.getUser() != null)
				.toList();
		if (linked.isEmpty()) {
			return;
		}
		Set<UUID> userIds = linked.stream()
				.map(m -> m.getUser().getId())
				.collect(Collectors.toSet());
		eventPublisher.publishEvent(new PermissionsChangedEvent(userIds));
	}
}
