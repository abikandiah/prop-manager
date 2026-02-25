package com.akandiah.propmanager.security;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.akandiah.propmanager.features.user.domain.User;
import com.akandiah.propmanager.features.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Single entry point for resolving the current authenticated user from a JWT.
 *
 * <ul>
 *   <li>{@link #resolve(Jwt)} — use in controllers where JWT is injected via
 *       {@code @AuthenticationPrincipal}. Throws 401 if user is not provisioned.</li>
 *   <li>{@link #resolve()} — use in services that read from {@code SecurityContextHolder}
 *       directly. Throws 401 if no JWT principal or user is not provisioned.</li>
 *   <li>{@link #resolveOptional(Jwt)} — use in auth filters / permission checks where a
 *       missing user means "deny" rather than an error.</li>
 *   <li>{@link #resolveOptionalId()} — use where only the user ID is needed and a missing
 *       user is non-fatal (e.g. audit/isolation best-effort checks).</li>
 * </ul>
 *
 * <p>Sync of JWT claims (name, email, lastLoggedInAt) happens exclusively at
 * {@code GET /me} via {@code UserService#getOrCreateUser}. All other endpoints use this
 * read-only resolver and expect the user to already be provisioned.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtUserResolver {

	private final UserService userService;

	/**
	 * Resolves the user from an already-injected JWT.
	 *
	 * @throws InsufficientAuthenticationException (→ 401) if the user has not yet been
	 *         provisioned via {@code GET /me}.
	 */
	public User resolve(Jwt jwt) {
		return userService.findUserFromJwt(jwt)
				.orElseThrow(() -> new InsufficientAuthenticationException(
						"User account not provisioned. Please call GET /me to set up your account."));
	}

	/**
	 * Resolves the user from the current {@link SecurityContextHolder}.
	 *
	 * @throws InsufficientAuthenticationException (→ 401) if no JWT principal is present
	 *         or the user has not yet been provisioned.
	 */
	public User resolve() {
		return resolve(extractJwt());
	}

	/**
	 * Non-throwing variant for auth filters and permission checks where a missing user
	 * should mean "deny" rather than an error response.
	 */
	public Optional<User> resolveOptional(Jwt jwt) {
		return userService.findUserFromJwt(jwt);
	}

	/**
	 * Non-throwing variant that pulls from {@link SecurityContextHolder} and returns only
	 * the user ID. Returns empty if there is no JWT principal or the user is not provisioned.
	 */
	public Optional<UUID> resolveOptionalId() {
		try {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			if (auth instanceof JwtAuthenticationToken jwtAuth) {
				return userService.findUserFromJwt(jwtAuth.getToken()).map(User::getId);
			}
		} catch (Exception e) {
			log.warn("Could not resolve current user id from security context", e);
		}
		return Optional.empty();
	}

	private Jwt extractJwt() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
			throw new InsufficientAuthenticationException("No authenticated JWT found in security context.");
		}
		return jwtAuth.getToken();
	}
}
