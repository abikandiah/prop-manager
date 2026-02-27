package com.akandiah.propmanager.config;

import java.util.List;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import static org.mockito.Mockito.mock;

/**
 * Test-only security config: provides a mock {@link JwtDecoder} so tests run
 * without a real OIDC issuer. Any non-blank Bearer token is accepted and
 * mapped to a principal with claim {@code sub=test-user} and
 * {@code groups=[USER]}.
 * <p>
 * Import via {@code @Import(TestSecurityConfig.class)} on test classes.
 */
@TestConfiguration
public class TestSecurityConfig {

    /** Satisfies EmailNotificationService in tests */
    @Bean
    JavaMailSender javaMailSender() {
        return mock(JavaMailSender.class);
    }

	/**
	 * Decoder that accepts any non-blank token and returns a minimal JWT with
	 * claims compatible with {@link JwtConfig} (e.g. "groups" for ROLE_USER).
	 */
	@Bean
	JwtDecoder jwtDecoder() {
		return token -> {
			if (token == null || token.isBlank()) {
				throw new JwtException("Missing or empty token");
			}
			return Jwt.withTokenValue(token)
					.header("alg", "RS256")
					.claim("iss", "https://test")
					.claim("sub", "test-user")
					.claim("groups", List.of("USER"))
					.build();
		};
	}
}
