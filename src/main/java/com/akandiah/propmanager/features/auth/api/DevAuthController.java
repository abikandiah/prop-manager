package com.akandiah.propmanager.features.auth.api;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.akandiah.propmanager.common.permission.AccessEntry;
import com.akandiah.propmanager.config.DevJwtConfig;
import com.akandiah.propmanager.features.auth.service.JwtHydrationService;
import com.akandiah.propmanager.features.user.domain.UserRepository;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ONLY active in 'dev' profile.
 * Provides a way to generate JWTs locally for testing.
 */
@RestController
@RequestMapping("/api/dev")
@Profile("dev")
@Tag(name = "Dev Auth", description = "Development-only auth utilities")
@Slf4j
@RequiredArgsConstructor
public class DevAuthController {

	private final DevJwtConfig devJwtConfig;
	private final UserRepository userRepository;
	private final JwtHydrationService jwtHydrationService;

	@Value("${app.jwt.secret:dev-secret-key-at-least-32-chars-long-123456}")
	private String jwtSecret;

	@PostMapping("/login")
	@Operation(summary = "Generate a dev JWT", description = "Generates a signed JWT with specified roles. ONLY for local development.")
	public Map<String, String> login(@Valid @RequestBody DevLoginRequest request) throws Exception {
		log.warn("DEV LOGIN ATTEMPT - User: {}", request.email());

		if (!devJwtConfig.getDevLoginSecret().equals(request.password())) {
			log.error("DEV LOGIN FAILED - Invalid password for user: {}", request.email());
			throw new BadCredentialsException("Invalid dev secret");
		}

		List<Map<String, Object>> accessClaim = new ArrayList<>();
		userRepository.findByEmail(request.email())
				.ifPresent(user -> jwtHydrationService.hydrate(user.getId()).stream()
						.map(AccessEntry::toClaimMap)
						.forEach(accessClaim::add));

		JWSSigner signer = new MACSigner(jwtSecret.getBytes());

		JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
				.subject(request.email())
				.issuer("https://prop-manager-dev")
				.expirationTime(new Date(new Date().getTime() + 3600 * 1000)) // 1 hour
				.claim("name", request.email())
				.claim("email", request.email())
				.claim("groups", request.roles());
		if (!accessClaim.isEmpty()) {
			claimsBuilder.claim("access", accessClaim);
		}
		JWTClaimsSet claimsSet = claimsBuilder.build();

		SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
		signedJWT.sign(signer);

		return Map.of("token", signedJWT.serialize());
	}

	@PostMapping("/token/refresh")
	@Operation(summary = "Refresh dev JWT", description = "Evicts the permissions cache and re-issues a JWT with fresh access claims. Uses the existing token as the credential. ONLY for local development.")
	public ResponseEntity<Map<String, String>> refreshToken(Authentication authentication) throws Exception {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BadCredentialsException("Valid JWT required to refresh token");
		}

		String email = authentication.getName();
		log.info("DEV TOKEN REFRESH - User: {}", email);

		var user = userRepository.findByEmail(email)
				.orElseThrow(() -> new BadCredentialsException("User not found: " + email));

		// Bust the Caffeine cache so hydrate() re-reads from DB (picks up new org)
		jwtHydrationService.evict(user.getId());

		List<Map<String, Object>> accessClaim = jwtHydrationService.hydrate(user.getId()).stream()
				.map(AccessEntry::toClaimMap)
				.collect(Collectors.toList());

		// Preserve the original groups (strip ROLE_ prefix added by Spring Security)
		List<String> groups = authentication.getAuthorities().stream()
				.map(a -> a.getAuthority().replaceFirst("^ROLE_", ""))
				.collect(Collectors.toList());

		JWSSigner signer = new MACSigner(jwtSecret.getBytes());

		JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
				.subject(email)
				.issuer("https://prop-manager-dev")
				.expirationTime(new Date(new Date().getTime() + 3600 * 1000)) // 1 hour
				.claim("name", email)
				.claim("email", email)
				.claim("groups", groups);
		if (!accessClaim.isEmpty()) {
			claimsBuilder.claim("access", accessClaim);
		}
		JWTClaimsSet claimsSet = claimsBuilder.build();

		SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
		signedJWT.sign(signer);

		return ResponseEntity.ok(Map.of("token", signedJWT.serialize()));
	}

	public record DevLoginRequest(
			@NotBlank @Email String email,
			@NotBlank String password,
			List<String> roles) {

		public DevLoginRequest {
			roles = roles != null ? roles : List.of();
		}
	}
}
