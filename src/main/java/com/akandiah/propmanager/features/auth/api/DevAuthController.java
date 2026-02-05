package com.akandiah.propmanager.features.auth.api;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.akandiah.propmanager.config.DevJwtConfig;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

	@Value("${app.jwt.secret:dev-secret-key-at-least-32-chars-long-123456}")
	private String jwtSecret;

	@PostMapping("/login")
	@Operation(summary = "Generate a dev JWT", description = "Generates a signed JWT with specified roles. ONLY for local development.")
	public Map<String, String> login(@RequestBody DevLoginRequest request) throws Exception {
		log.warn("DEV LOGIN ATTEMPT - User: {}", request.username());

		if (!devJwtConfig.getDevLoginSecret().equals(request.password())) {
			log.error("DEV LOGIN FAILED - Invalid password for user: {}", request.username());
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid dev secret");
		}

		JWSSigner signer = new MACSigner(jwtSecret.getBytes());

		JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
				.subject(request.username())
				.issuer("prop-manager-dev")
				.expirationTime(new Date(new Date().getTime() + 3600 * 1000)) // 1 hour
				.claim("name", request.username())
				.claim("email", request.username() + "@example.com")
				.claim("groups", request.roles()) // Matches JwtConfig
				.build();

		SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
		signedJWT.sign(signer);

		return Map.of("access_token", signedJWT.serialize());
	}

	public record DevLoginRequest(String username, String password, List<String> roles) {
	}
}
