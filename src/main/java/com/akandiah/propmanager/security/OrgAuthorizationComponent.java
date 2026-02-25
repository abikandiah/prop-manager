package com.akandiah.propmanager.security;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.akandiah.propmanager.features.membership.domain.MembershipRepository;

import lombok.RequiredArgsConstructor;

/** SpEL bean for @PreAuthorize: checks whether the current user is a member of an organization. */
@Component("orgAuthz")
@RequiredArgsConstructor
public class OrgAuthorizationComponent {

	private final MembershipRepository membershipRepository;
	private final JwtUserResolver jwtUserResolver;

	public boolean isMember(UUID orgId, Authentication auth) {
		if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
			return false;
		}
		return jwtUserResolver.resolveOptional(jwtAuth.getToken())
				.map(user -> membershipRepository.existsByUserIdAndOrganizationId(user.getId(), orgId))
				.orElse(false);
	}
}
