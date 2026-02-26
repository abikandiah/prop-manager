package com.akandiah.propmanager.features.invite.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.common.permission.AccessEntry;
import com.akandiah.propmanager.common.permission.Actions;
import com.akandiah.propmanager.common.permission.PermissionDomains;
import com.akandiah.propmanager.common.permission.ResourceType;
import com.akandiah.propmanager.features.invite.domain.Invite;
import com.akandiah.propmanager.features.invite.domain.InviteRepository;
import com.akandiah.propmanager.features.invite.domain.TargetType;
import com.akandiah.propmanager.features.lease.domain.Lease;
import com.akandiah.propmanager.features.lease.domain.LeaseRepository;
import com.akandiah.propmanager.features.user.domain.User;
import com.akandiah.propmanager.common.util.SecurityUtils;
import com.akandiah.propmanager.security.HierarchyAwareAuthorizationService;
import com.akandiah.propmanager.security.JwtUserResolver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for checking invite-related permissions.
 * Used by @PreAuthorize annotations in controllers.
 */
@Service("inviteAuthService")
@Slf4j
@RequiredArgsConstructor
public class InviteAuthorizationService {

	private final LeaseRepository leaseRepository;
	private final InviteRepository inviteRepository;
	private final HierarchyAwareAuthorizationService authorizationService;
	private final JwtUserResolver jwtUserResolver;

	/**
	 * Check if the current user can create an invite for the given target resource.
	 * For LEASE targets, requires CREATE on leases domain at UNIT scope.
	 * For MEMBERSHIP targets, requires CREATE on tenants domain at ORG scope.
	 * Admins are always allowed.
	 */
	public boolean canCreateInviteForTarget(TargetType targetType, UUID targetId) {
		try {
			if (SecurityUtils.isGlobalAdmin())
				return true;
			return checkTargetAccess(targetType, targetId, Actions.CREATE);
		} catch (Exception e) {
			log.error("Error checking invite creation permission for target {} {}", targetType, targetId, e);
			return false;
		}
	}

	/**
	 * Check if the current user can manage (resend/revoke) a specific invite.
	 * Resolves the target from the invite and delegates to
	 * canCreateInviteForTarget.
	 */
	public boolean canManageInvite(UUID inviteId) {
		try {
			if (SecurityUtils.isGlobalAdmin()) {
				return true;
			}

			User currentUser = jwtUserResolver.resolve();
			Invite invite = inviteRepository.findById(inviteId)
					.orElseThrow(() -> new ResourceNotFoundException("Invite", inviteId));

			// User created this invite
			if (invite.getInvitedBy().getId().equals(currentUser.getId())) {
				return true;
			}

			// User has access to the invite's target resource
			if (canViewTarget(invite.getTargetType(), invite.getTargetId())) {
				return true;
			}

			log.warn("User {} attempted to manage invite {} without permission", currentUser.getId(), inviteId);
			return false;

		} catch (Exception e) {
			log.error("Error checking invite management permission for invite {}", inviteId, e);
			return false;
		}
	}

	/**
	 * Check if the current user can view invites for the given target resource.
	 * For LEASE targets, requires READ on leases domain at UNIT scope.
	 * For MEMBERSHIP targets, requires READ on tenants domain at ORG scope.
	 * Admins are always allowed.
	 */
	public boolean canViewInvitesForTarget(TargetType targetType, UUID targetId) {
		try {
			if (SecurityUtils.isGlobalAdmin())
				return true;
			return checkTargetAccess(targetType, targetId, Actions.READ);
		} catch (Exception e) {
			log.error("Error checking invite view permission for target {} {}", targetType, targetId, e);
			return false;
		}
	}

	/**
	 * Check if the current user can view a specific invite.
	 * A user can view an invite if they:
	 * - Created the invite
	 * - Are the invited person (email matches their email)
	 * - Can manage the invite (see canManageInvite)
	 *
	 * @param inviteId Invite ID to check
	 * @return true if authorized, false otherwise
	 */
	public boolean canViewInvite(UUID inviteId) {
		try {
			User currentUser = jwtUserResolver.resolve();
			Invite invite = inviteRepository.findById(inviteId)
					.orElseThrow(() -> new ResourceNotFoundException("Invite", inviteId));

			// User is the invited person
			if (invite.getEmail().equalsIgnoreCase(currentUser.getEmail())) {
				return true;
			}

			// User can manage this invite
			return canManageInvite(inviteId);

		} catch (Exception e) {
			log.error("Error checking invite view permission for invite {}", inviteId, e);
			return false;
		}
	}

	// ───────────────────────── Private helpers ─────────────────────────

	/**
	 * Check if the current user has the required action on the given target.
	 * For LEASE: resolves the unit and org, checks via hierarchy-aware auth.
	 * For MEMBERSHIP: targetId is the org ID, checks org-level tenants domain.
	 */
	private boolean checkTargetAccess(TargetType targetType, UUID targetId, int requiredAction) {
		return switch (targetType) {
			case LEASE -> {
				Lease lease = leaseRepository.findByIdWithUnitPropAndOrg(targetId).orElse(null);
				if (lease == null)
					yield false;
				UUID unitId = lease.getUnit().getId();
				UUID orgId = lease.getUnit().getProp().getOrganization() != null
						? lease.getUnit().getProp().getOrganization().getId()
						: null;
				if (orgId == null)
					yield false;
				List<AccessEntry> access = SecurityUtils.getAccessFromRequest();
				yield authorizationService.allow(access, requiredAction, PermissionDomains.LEASES,
						ResourceType.UNIT, unitId, orgId);
			}
			case MEMBERSHIP -> {
				// targetId is the org ID
				List<AccessEntry> access = SecurityUtils.getAccessFromRequest();
				yield authorizationService.allow(access, requiredAction, PermissionDomains.ORGANIZATION,
						ResourceType.ORG, targetId, targetId);
			}
		};
	}

	/**
	 * Convenience wrapper used by canManageInvite — checks READ access on the
	 * invite's target.
	 */
	private boolean canViewTarget(TargetType targetType, UUID targetId) {
		return checkTargetAccess(targetType, targetId, Actions.READ);
	}

}
