package com.akandiah.propmanager.features.invite.service;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
import com.akandiah.propmanager.features.user.service.UserService;
import com.akandiah.propmanager.security.HierarchyAwareAuthorizationService;
import com.akandiah.propmanager.security.JwtAccessHydrationFilter;

import jakarta.servlet.http.HttpServletRequest;
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

	private final InviteRepository inviteRepository;
	private final LeaseRepository leaseRepository;
	private final UserService userService;
	private final HierarchyAwareAuthorizationService authorizationService;

	/**
	 * Check if the current user can create an invite for the given target resource.
	 * For LEASE targets, requires CREATE on leases domain at UNIT scope.
	 * Admins are always allowed.
	 *
	 * @param targetType type of the target resource
	 * @param targetId   ID of the target resource
	 * @return true if authorized, false otherwise
	 */
	public boolean canCreateInviteForTarget(TargetType targetType, UUID targetId) {
		try {
			if (hasRole("ADMIN")) return true;
			return checkTargetAccess(targetType, targetId, Actions.CREATE);
		} catch (Exception e) {
			log.error("Error checking invite creation permission for target {} {}", targetType, targetId, e);
			return false;
		}
	}

	/**
	 * Check if the current user can view invites for the given target resource.
	 * For LEASE targets, requires READ on leases domain at UNIT scope.
	 * Admins are always allowed.
	 *
	 * @param targetType type of the target resource
	 * @param targetId   ID of the target resource
	 * @return true if authorized, false otherwise
	 */
	public boolean canViewInvitesForTarget(TargetType targetType, UUID targetId) {
		try {
			if (hasRole("ADMIN")) return true;
			return checkTargetAccess(targetType, targetId, Actions.READ);
		} catch (Exception e) {
			log.error("Error checking invite view permission for target {} {}", targetType, targetId, e);
			return false;
		}
	}

	/**
	 * Check if the current user can manage (resend, revoke) a specific invite.
	 * A user can manage an invite if they:
	 * - Created the invite (are the inviter)
	 * - Are an admin
	 * - Have access to the invite's target resource
	 *
	 * @param inviteId Invite ID to check
	 * @return true if authorized, false otherwise
	 */
	public boolean canManageInvite(UUID inviteId) {
		try {
			User currentUser = getCurrentUser();
			Invite invite = inviteRepository.findById(inviteId)
					.orElseThrow(() -> new ResourceNotFoundException("Invite", inviteId));

			// User created this invite
			if (invite.getInvitedBy().getId().equals(currentUser.getId())) {
				return true;
			}

			// User is admin
			if (hasRole("ADMIN")) {
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
			User currentUser = getCurrentUser();
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
	 * For LEASE targets: resolves the unit and org, then checks via hierarchy-aware auth.
	 */
	private boolean checkTargetAccess(TargetType targetType, UUID targetId, int requiredAction) {
		return switch (targetType) {
			case LEASE -> {
				Lease lease = leaseRepository.findByIdWithUnitPropAndOrg(targetId).orElse(null);
				if (lease == null) yield false;
				UUID unitId = lease.getUnit().getId();
				UUID orgId = lease.getUnit().getProp().getOrganization() != null
						? lease.getUnit().getProp().getOrganization().getId() : null;
				if (orgId == null) yield false;
				List<AccessEntry> access = getAccessFromRequest();
				yield authorizationService.allow(access, requiredAction, PermissionDomains.LEASES,
						ResourceType.UNIT, unitId, orgId);
			}
		};
	}

	/**
	 * Convenience wrapper used by canManageInvite — checks READ access on the invite's target.
	 */
	private boolean canViewTarget(TargetType targetType, UUID targetId) {
		return checkTargetAccess(targetType, targetId, Actions.READ);
	}

	/**
	 * Get the currently authenticated user.
	 */
	private User getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
			throw new IllegalStateException("No authenticated user found");
		}

		Jwt jwt = (Jwt) authentication.getPrincipal();
		return userService.findUserFromJwt(jwt)
				.orElseThrow(() -> new IllegalStateException("User not found for authenticated subject"));
	}

	/**
	 * Check if the current user has a specific role.
	 */
	private boolean hasRole(String role) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null) {
			return false;
		}

		return authentication.getAuthorities().stream()
				.anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
	}

	@SuppressWarnings("unchecked")
	private List<AccessEntry> getAccessFromRequest() {
		ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		if (attrs == null) {
			return List.of();
		}
		HttpServletRequest request = attrs.getRequest();
		Object attr = request.getAttribute(JwtAccessHydrationFilter.REQUEST_ATTRIBUTE_ACCESS);
		return attr instanceof List<?> list ? (List<AccessEntry>) list : List.of();
	}
}
