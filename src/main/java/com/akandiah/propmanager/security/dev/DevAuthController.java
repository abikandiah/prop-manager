package com.akandiah.propmanager.security.dev;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.akandiah.propmanager.config.JwtProperties;
import com.akandiah.propmanager.security.dev.dto.DevTokenRequest;
import com.akandiah.propmanager.security.dev.dto.DevTokenResponse;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Dev profile only: REST endpoint to exchange a dev username + password for a
 * JWT. The password must match the secure-random key logged at server startup.
 * Used by the frontend SPA during local development.
 */
@RestController
@RequestMapping("/dev")
@Profile("dev")
@Tag(name = "Dev Auth", description = "Development-only JWT token endpoint (dev profile)")
@RequiredArgsConstructor
@Slf4j
public class DevAuthController {

	private final String devAuthPassword;
	private final JwtProperties jwtProperties;

	@PostMapping("/token")
	@Operation(summary = "Get dev JWT", description = "Exchange dev username + startup password for a JWT. Dev profile only.")
	public ResponseEntity<DevTokenResponse> token(@Valid @RequestBody DevTokenRequest request) {
		if (devAuthPassword == null || !constantTimeEquals(devAuthPassword, request.password())) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		String token = buildToken(request.username());
		return ResponseEntity.ok(new DevTokenResponse(token));
	}

	private String buildToken(String subject) {
		return Jwts.builder()
				.header()
				.add("typ", "JWT")
				.and()
				.subject(subject)
				.issuer(jwtProperties.issuer())
				.issuedAt(new Date())
				.expiration(new Date(System.currentTimeMillis() + jwtProperties.expirationMs()))
				.claim("groups", List.of("ADMIN"))
				.signWith(Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8)))
				.compact();
	}

	private static boolean constantTimeEquals(String a, String b) {
		byte[] aa = a.getBytes(StandardCharsets.UTF_8);
		byte[] bb = b.getBytes(StandardCharsets.UTF_8);
		return MessageDigest.isEqual(digest(aa), digest(bb));
	}

	private static byte[] digest(byte[] input) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(input);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 unavailable", e);
		}
	}
}
