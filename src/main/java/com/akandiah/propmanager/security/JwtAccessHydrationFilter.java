package com.akandiah.propmanager.security;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import com.akandiah.propmanager.common.permission.AccessEntry;
import com.akandiah.propmanager.features.auth.service.JwtHydrationService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Hydrates the access list from JWT claim (dev) or DB (prod) and sets it as a
 * request attribute.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAccessHydrationFilter extends OncePerRequestFilter {

	/**
	 * Request attribute for the hydrated access list. Use this in authorization
	 * checks.
	 */
	public static final String REQUEST_ATTRIBUTE_ACCESS = "com.akandiah.propmanager.jwt.access";

	private final JwtUserResolver jwtUserResolver;
	private final JwtHydrationService jwtHydrationService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		Authentication auth = org.springframework.security.core.context.SecurityContextHolder
				.getContext()
				.getAuthentication();

		if (auth instanceof JwtAuthenticationToken jwtAuth && auth.isAuthenticated()) {
			List<AccessEntry> access = getAccessFromToken(jwtAuth);
			if (access == null) {
				access = hydrateFromDb(jwtAuth);
			}
			if (access != null && !access.isEmpty()) {
				request.setAttribute(REQUEST_ATTRIBUTE_ACCESS, access);
			}
		}

		filterChain.doFilter(request, response);
	}

	private List<AccessEntry> getAccessFromToken(JwtAuthenticationToken jwtAuth) {
		try {
			Object claim = jwtAuth.getToken().getClaim("access");
			if (claim instanceof List<?> list) {
				return AccessEntry.fromClaimList(list);
			}
		} catch (Exception e) {
			log.trace("No or invalid access claim in JWT", e);
		}
		return null;
	}

	private List<AccessEntry> hydrateFromDb(JwtAuthenticationToken jwtAuth) {
		return jwtUserResolver.resolveOptional(jwtAuth.getToken())
				.map(user -> jwtHydrationService.hydrate(user.getId()))
				.orElse(List.of());
	}
}
