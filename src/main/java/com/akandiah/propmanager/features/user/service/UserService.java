package com.akandiah.propmanager.features.user.service;

import java.time.Instant;
import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.features.user.domain.User;
import com.akandiah.propmanager.features.user.domain.UserIdentity;
import com.akandiah.propmanager.features.user.domain.UserIdentityRepository;
import com.akandiah.propmanager.features.user.domain.UserRegisteredEvent;
import com.akandiah.propmanager.features.user.domain.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * User and identity resolution. Accounts are keyed by email; multiple OIDC
 * identities (issuer + sub) can link to the same user so that signing in with
 * Google and then Meta (same email) uses one account.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

	private static final String ISSUER_LEGACY_OR_MISSING = "legacy";

	private final UserRepository userRepository;
	private final UserIdentityRepository userIdentityRepository;
	private final ApplicationEventPublisher eventPublisher;

	/**
	 * Resolves the user for the given OIDC identity (issuer + subject).
	 */
	@Transactional(readOnly = true)
	private Optional<User> findUserByIdentity(String issuer, String sub) {
		return userIdentityRepository.findByIssuerAndSubWithUser(normalizeIssuer(issuer), sub)
				.map(UserIdentity::getUser);
	}

	/**
	 * Resolves the current user from the JWT. Uses issuer and subject; if issuer
	 * is missing (e.g. legacy or test tokens), uses a fallback so lookup still works.
	 */
	@Transactional(readOnly = true)
	public Optional<User> findUserFromJwt(Jwt jwt) {
		String issuer = normalizeIssuer(getIssuerString(jwt));
		String sub = jwt.getSubject();
		if (sub == null || sub.isBlank()) {
			return Optional.empty();
		}
		return findUserByIdentity(issuer, sub);
	}

	/**
	 * Issuer string from JWT. Prefers {@link Jwt#getIssuer()} (spec-correct); falls back to
	 * {@code iss} claim as string when conversion to URL fails (e.g. some dev decoders).
	 */
	public static String getIssuerString(Jwt jwt) {
		try {
			if (jwt.getIssuer() != null) {
				return jwt.getIssuer().toString();
			}
		} catch (Exception ignored) {
			// e.g. IllegalArgumentException when iss cannot be converted to URL
		}
		return jwt.getClaimAsString("iss");
	}

	private static String normalizeIssuer(String issuer) {
		return (issuer != null && !issuer.isBlank()) ? issuer : ISSUER_LEGACY_OR_MISSING;
	}

	/**
	 * Gets the current user by identity, or auto-creates one with terms_accepted = false.
	 * Updates name/email and last_logged_in_at when the user already exists.
	 */
	@Transactional
	public User getOrCreateUser(String issuer, String sub, String name, String email) {
		String normIssuer = normalizeIssuer(issuer);
		Instant now = Instant.now();
		// 1) Already linked identity
		Optional<User> byIdentity = findUserByIdentity(normIssuer, sub);
		if (byIdentity.isPresent()) {
			User user = byIdentity.get();
			if (name != null) {
				user.setName(name);
			}
			if (email != null) {
				user.setEmail(email);
			}
			user.setLastLoggedInAt(now);
			return userRepository.save(user);
		}
		// 2) Same email → link this IdP to existing account
		Optional<User> byEmail = userRepository.findByEmail(email);
		if (byEmail.isPresent()) {
			User user = byEmail.get();
			log.info("Linking identity {} to existing account: {}", normIssuer, email);
			userIdentityRepository.save(UserIdentity.builder()
					.user(user)
					.issuer(normIssuer)
					.sub(sub)
					.build());
			if (name != null) {
				user.setName(name);
			}
			user.setLastLoggedInAt(now);
			return userRepository.save(user);
		}
		// 3) New user: auto-create with terms_accepted = false
		log.info("Auto-creating user: {}", email);
		String displayName = (name != null && !name.isBlank()) ? name.trim() : "User " + sub;
		User newUser = User.builder()
				.name(displayName)
				.email(email)
				.termsAccepted(false)
				.lastLoggedInAt(now)
				.build();
		User saved = userRepository.save(newUser);
		userIdentityRepository.save(UserIdentity.builder()
				.user(saved)
				.issuer(normIssuer)
				.sub(sub)
				.build());
		eventPublisher.publishEvent(new UserRegisteredEvent(saved.getId()));
		return saved;
	}

	@Transactional
	public User updateTermsAccepted(User user, boolean termsAccepted) {
		user.setTermsAccepted(termsAccepted);
		return userRepository.save(user);
	}
}
