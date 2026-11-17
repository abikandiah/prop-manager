package com.akandiah.propmanager.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtConfigTest {

	private static final String AUDIENCE = "https://api.example.test";

	private static Jwt.Builder jwt() {
		return Jwt.withTokenValue("t").header("alg", "none").subject("user")
				.issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60));
	}

	private static List<String> authorities(Jwt token) {
		return new JwtConfig().jwtAuthenticationConverter().convert(token).getAuthorities().stream()
				.map(GrantedAuthority::getAuthority).toList();
	}

	@Test
	void mapsRolesClaimToRoleAuthorities() {
		assertThat(authorities(jwt().claim(JwtConfig.ROLES_CLAIM, List.of("ADMIN")).build()))
				.contains("ROLE_ADMIN");
	}

	@Test
	void ignoresLegacyGroupsClaim() {
		assertThat(authorities(jwt().claim("groups", List.of("ADMIN")).build()))
				.doesNotContain("ROLE_ADMIN");
	}

	@Test
	void acceptsTokenIssuedForThisApi() {
		Jwt token = jwt().audience(List.of(AUDIENCE)).build();
		assertThat(JwtConfig.audienceValidator(AUDIENCE).validate(token).hasErrors()).isFalse();
	}

	@Test
	void rejectsTokenIssuedForAnotherApp() {
		Jwt token = jwt().audience(List.of("https://other-app.example.test")).build();
		assertThat(JwtConfig.audienceValidator(AUDIENCE).validate(token).hasErrors()).isTrue();
	}

	@Test
	void rejectsTokenWithoutAudience() {
		assertThat(JwtConfig.audienceValidator(AUDIENCE).validate(jwt().build()).hasErrors()).isTrue();
	}
}
