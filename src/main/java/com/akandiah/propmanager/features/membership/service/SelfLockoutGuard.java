package com.akandiah.propmanager.features.membership.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.akandiah.propmanager.common.exception.SelfLockoutException;
import com.akandiah.propmanager.common.permission.Actions;
import com.akandiah.propmanager.common.permission.PermissionDomains;
import com.akandiah.propmanager.common.permission.PermissionMaskUtil;
import com.akandiah.propmanager.common.permission.ResourceType;
import com.akandiah.propmanager.common.util.SecurityUtils;
import com.akandiah.propmanager.features.membership.domain.Membership;
import com.akandiah.propmanager.features.membership.domain.MembershipRepository;
import com.akandiah.propmanager.features.membership.domain.PermissionPolicy;
import com.akandiah.propmanager.features.membership.domain.PolicyAssignment;
import com.akandiah.propmanager.features.membership.domain.PolicyAssignmentRepository;
import com.akandiah.propmanager.security.JwtUserResolver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Centralized guard preventing users from accidentally locking themselves out of an
 * organization, and preventing orgs from becoming unmanageable (orphaned-org protection).
 *
 * <p>All checks are bypassed for {@code ROLE_ADMIN} users, who have system-level backdoor
 * access and cannot be truly locked out of any organization.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SelfLockoutGuard {

	private final PolicyAssignmentRepository assignmentRepository;
	private final MembershipRepository membershipRepository;
	private final JwtUserResolver jwtUserResolver;

	/**
	 * Prevents a user from removing their own last ORG-scoped org-read access via an
	 * assignment change (update or delete).
	 *
	 * @param assignment the assignment being mutated
	 * @param newOverrides the overrides that will apply after the change (null for delete)
	 * @param newPolicy the policy that will apply after the change (null for delete)
	 */
	public void requireNoSelfLockout(PolicyAssignment assignment, Map<String, String> newOverrides,
			PermissionPolicy newPolicy) {
		if (assignment.getResourceType() != ResourceType.ORG) {
			return;
		}
		if (SecurityUtils.isGlobalAdmin()) {
			return;
		}

		UUID membershipUserId = assignment.getMembership().getUser() != null
				? assignment.getMembership().getUser().getId()
				: null;
		UUID currentUserId = jwtUserResolver.resolveOptionalId().orElse(null);
		if (currentUserId == null || !currentUserId.equals(membershipUserId)) {
			return;
		}

		UUID membershipId = assignment.getMembership().getId();
		UUID orgId = assignment.getMembership().getOrganization().getId();

		boolean otherAssignmentGrantsOrgRead = assignmentRepository
				.findByMembershipIdInAndResourceTypeWithPolicy(List.of(membershipId), ResourceType.ORG)
				.stream()
				.filter(a -> !a.getId().equals(assignment.getId()))
				.anyMatch(a -> hasOrgRead(a.getOverrides(), a.getPolicy()));

		boolean grantsOrgReadAfterChange = hasOrgRead(newOverrides, newPolicy);

		if (!otherAssignmentGrantsOrgRead && !grantsOrgReadAfterChange) {
			log.warn("Blocked self-lockout via assignment change: userId={}, orgId={}, assignmentId={}",
					currentUserId, orgId, assignment.getId());
			throw new SelfLockoutException();
		}
	}

	/**
	 * Prevents deleting a membership that would self-lock the caller out of the org,
	 * or leave the organization with no remaining ORG:READ member (orphaned-org protection).
	 */
	public void requireNoSelfLockoutOnMembershipDeletion(Membership membership) {
		if (SecurityUtils.isGlobalAdmin()) {
			return;
		}

		UUID orgId = membership.getOrganization().getId();
		UUID membershipUserId = membership.getUser() != null ? membership.getUser().getId() : null;
		UUID currentUserId = jwtUserResolver.resolveOptionalId().orElse(null);

		// Self-lockout: always block a user from deleting their own membership.
		// Even if they have other memberships in the same org, the membership row
		// holds their policy assignments; removing it drops all their org access.
		if (currentUserId != null && currentUserId.equals(membershipUserId)) {
			log.warn("Blocked self-membership-deletion: userId={}, orgId={}, membershipId={}",
					currentUserId, orgId, membership.getId());
			throw new SelfLockoutException();
		}

		// Orphaned-org: if this membership carries the only ORG:READ access in the org,
		// block its deletion regardless of who is performing the action.
		boolean membershipHasOrgRead = assignmentRepository
				.findByMembershipIdInAndResourceTypeWithPolicy(List.of(membership.getId()), ResourceType.ORG)
				.stream()
				.anyMatch(a -> hasOrgRead(a.getOverrides(), a.getPolicy()));

		if (membershipHasOrgRead) {
			List<UUID> otherMembershipIds = membershipRepository.findByOrganizationId(orgId)
					.stream()
					.filter(m -> !m.getId().equals(membership.getId()))
					.map(Membership::getId)
					.toList();

			boolean anyOtherHasOrgRead = !otherMembershipIds.isEmpty() && assignmentRepository
					.findByMembershipIdInAndResourceTypeWithPolicy(otherMembershipIds, ResourceType.ORG)
					.stream()
					.anyMatch(a -> hasOrgRead(a.getOverrides(), a.getPolicy()));

			if (!anyOtherHasOrgRead) {
				log.warn("Blocked orphaned-org membership-deletion: orgId={}, membershipId={}",
						orgId, membership.getId());
				throw new SelfLockoutException(
						"Removing this member would leave the organization with no administrators. Assign another member as admin first.");
			}
		}
	}

	/**
	 * Prevents updating a shared policy in a way that removes the caller's own ORG:READ
	 * access. Simulates the post-update state by substituting {@code newPermissions} for
	 * any assignment that references the given policy.
	 *
	 * @param policyId       the policy being updated
	 * @param newPermissions the proposed new permissions map
	 * @param orgId          the org context of the update
	 */
	public void requireNoSelfLockoutOnPolicyUpdate(UUID policyId, Map<String, String> newPermissions, UUID orgId) {
		if (orgId == null || SecurityUtils.isGlobalAdmin()) {
			return;
		}
		UUID currentUserId = jwtUserResolver.resolveOptionalId().orElse(null);
		if (currentUserId == null) {
			return;
		}

		Membership userMembership = membershipRepository
				.findByUserIdAndOrganizationId(currentUserId, orgId)
				.orElse(null);
		if (userMembership == null) {
			return;
		}
		List<UUID> membershipIds = List.of(userMembership.getId());

		List<PolicyAssignment> orgAssignments = assignmentRepository
				.findByMembershipIdInAndResourceTypeWithPolicy(membershipIds, ResourceType.ORG);

		// Simulate the update: for assignments using this policy, apply newPermissions
		boolean stillHasOrgRead = orgAssignments.stream().anyMatch(a -> {
			if (a.getPolicy() != null && a.getPolicy().getId().equals(policyId)) {
				return hasOrgRead(a.getOverrides(), newPermissions);
			}
			return hasOrgRead(a.getOverrides(), a.getPolicy());
		});

		if (!stillHasOrgRead) {
			log.warn("Blocked self-lockout via policy update: userId={}, orgId={}, policyId={}",
					currentUserId, orgId, policyId);
			throw new SelfLockoutException();
		}
	}

	private boolean hasOrgRead(Map<String, String> overrides, PermissionPolicy policy) {
		String orgPerms = null;
		if (overrides != null) {
			orgPerms = overrides.get(PermissionDomains.ORGANIZATION);
		}
		if (orgPerms == null && policy != null) {
			orgPerms = policy.getPermissions().get(PermissionDomains.ORGANIZATION);
		}
		return orgPerms != null && PermissionMaskUtil.hasAccess(PermissionMaskUtil.parseToMask(orgPerms), Actions.READ);
	}

	/** Variant used when simulating a policy update (permissions not yet persisted). */
	private boolean hasOrgRead(Map<String, String> overrides, Map<String, String> policyPermissions) {
		String orgPerms = null;
		if (overrides != null) {
			orgPerms = overrides.get(PermissionDomains.ORGANIZATION);
		}
		if (orgPerms == null && policyPermissions != null) {
			orgPerms = policyPermissions.get(PermissionDomains.ORGANIZATION);
		}
		return orgPerms != null && PermissionMaskUtil.hasAccess(PermissionMaskUtil.parseToMask(orgPerms), Actions.READ);
	}
}
