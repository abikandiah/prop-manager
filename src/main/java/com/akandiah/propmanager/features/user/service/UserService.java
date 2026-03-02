package com.akandiah.propmanager.features.user.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.permission.AccessEntry;
import com.akandiah.propmanager.common.permission.Actions;
import com.akandiah.propmanager.common.permission.PermissionDomains;
import com.akandiah.propmanager.common.permission.ResourceType;
import com.akandiah.propmanager.features.user.api.dto.UiRole;
import com.akandiah.propmanager.features.user.domain.User;
import com.akandiah.propmanager.features.user.domain.UserIdentity;
import com.akandiah.propmanager.features.user.domain.UserIdentityRepository;
import com.akandiah.propmanager.features.user.domain.UserRegisteredEvent;
import com.akandiah.propmanager.features.user.domain.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** User and identity resolution. Multiple OIDC identities (issuer+sub) can link to one account via email. */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

	private static final String ISSUER_LEGACY_OR_MISSING = "legacy";

	private final UserRepository userRepository;
	private final UserIdentityRepository userIdentityRepository;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional(readOnly = true)
	private Optional<User> findUserByIdentity(String issuer, String sub) {
		return userIdentityRepository.findByIssuerAndSubWithUser(normalizeIssuer(issuer), sub)
				.map(UserIdentity::getUser);
	}

	@Transactional(readOnly = true)
	public Optional<User> findUserFromJwt(Jwt jwt) {
		String issuer = normalizeIssuer(getIssuerString(jwt));
		String sub = jwt.getSubject();
		if (sub == null || sub.isBlank()) {
			return Optional.empty();
		}
		return findUserByIdentity(issuer, sub);
	}

	/** Prefers {@link Jwt#getIssuer()}; falls back to {@code iss} claim string when URL conversion fails. */
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

	/** Resolves user by identity; links to existing account by email; auto-creates if new. */
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

	/** Derives UI roles from authorities + access list. UI hints only — not security enforcement. */
	public List<UiRole> deriveRoles(List<String> authorities, List<AccessEntry> access) {
		Set<UiRole> roles = new HashSet<>();

		if (authorities.contains("ROLE_ADMIN")) {
			roles.add(UiRole.ADMIN);
		}

		for (AccessEntry entry : access) {
			// Org admin/owner: has write authority over the organisation itself
			if (entry.scopeType() == ResourceType.ORG) {
				if (hasAction(entry, PermissionDomains.ORGANIZATION, Actions.UPDATE) ||
						hasAction(entry, PermissionDomains.ORGANIZATION, Actions.DELETE)) {
					roles.add(UiRole.ADMIN);
				}
			}
			// Manager: operational access to properties (ORG or PROPERTY scope)
			// Property owners land here via PROPERTY-scope entries — not ADMIN
			if (entry.scopeType() == ResourceType.ORG || entry.scopeType() == ResourceType.PROPERTY) {
				if (hasAction(entry, PermissionDomains.PORTFOLIO, Actions.READ)) {
					roles.add(UiRole.MANAGER);
				}
			}
			// Tenant: lease-level read access at unit scope
			if (entry.scopeType() == ResourceType.UNIT) {
				if (hasAction(entry, PermissionDomains.LEASES, Actions.READ)) {
					roles.add(UiRole.TENANT);
				}
			}
		}

		if (roles.isEmpty()) {
			roles.add(UiRole.OTHER);
		}
		return new ArrayList<>(roles);
	}

	private boolean hasAction(AccessEntry entry, String domain, int action) {
		int mask = entry.permissions().getOrDefault(domain, 0);
		return (mask & action) != 0;
	}
}
