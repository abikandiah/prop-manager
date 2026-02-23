package com.akandiah.propmanager.security;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.akandiah.propmanager.features.organization.domain.MembershipRepository;
import com.akandiah.propmanager.features.user.service.UserService;

import lombok.RequiredArgsConstructor;

/**
 * Bean for use in {@code @PreAuthorize} SpEL: checks whether the current user
 * is a member of the given organization.
 *
 * <p>Usage:
 * {@code @PreAuthorize("hasRole('ADMIN') or @orgAuthz.isMember(#id, authentication)")}
 *
 * <p>{@code hasRole('ADMIN')} = system-level platform operator. Org-level admins are
 * members with full-permission ORG scopes, not Java roles.
 */
@Component("orgAuthz")
@RequiredArgsConstructor
public class OrgAuthorizationComponent {

	private final MembershipRepository membershipRepository;
	private final UserService userService;

	/**
	 * Returns true if the authenticated user has a membership in the given org.
	 *
	 * @param orgId organization ID
	 * @param auth  current authentication
	 * @return true if member
	 */
	public boolean isMember(UUID orgId, Authentication auth) {
		if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
			return false;
		}
		return userService.findUserFromJwt(jwtAuth.getToken())
				.map(user -> membershipRepository.existsByUserIdAndOrganizationId(user.getId(), orgId))
				.orElse(false);
	}
}
