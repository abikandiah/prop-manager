package com.akandiah.propmanager.features.invite.service;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.features.invite.domain.Invite;
import com.akandiah.propmanager.features.invite.domain.InviteRepository;
import com.akandiah.propmanager.features.user.domain.User;
import com.akandiah.propmanager.features.user.domain.UserRepository;

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
	private final UserRepository userRepository;

	/**
	 * Check if the current user can manage (resend, revoke, view) a specific
	 * invite.
	 * A user can manage an invite if they:
	 * - Created the invite (are the inviter)
	 * - Are an admin
	 * - Own/manage the target resource (TODO: implement resource-level checks)
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

			// TODO: Add resource-level permission checks
			// e.g., user owns the property/lease that the invite is for
			// This would require looking up the target resource and checking ownership

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

	/**
	 * Get the currently authenticated user.
	 */
	private User getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
			throw new IllegalStateException("No authenticated user found");
		}

		Jwt jwt = (Jwt) authentication.getPrincipal();
		String userSub = jwt.getSubject();

		return userRepository.findByIdpSub(userSub)
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
}
