package com.akandiah.propmanager.security;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.akandiah.propmanager.common.util.SecurityUtils;
import com.akandiah.propmanager.features.membership.domain.MembershipRepository;

import lombok.RequiredArgsConstructor;

/**
 * SpEL bean for {@code @PreAuthorize}: checks whether the current user is a
 * member of an organization.
 */
@Component("orgGuard")
@RequiredArgsConstructor
public class OrgGuard {

	private final MembershipRepository membershipRepository;
	private final JwtUserResolver jwtUserResolver;

	/**
	 * Checks if the current user is a member of the given organization.
	 * Global Admins are always allowed.
	 */
	public boolean isMember(UUID orgId, Authentication auth) {
		if (SecurityUtils.isGlobalAdmin()) {
			return true;
		}
		if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
			return false;
		}
		return jwtUserResolver.resolveOptional(jwtAuth.getToken())
				.map(user -> membershipRepository.existsByUserIdAndOrganizationId(user.getId(), orgId))
				.orElse(false);
	}
}
