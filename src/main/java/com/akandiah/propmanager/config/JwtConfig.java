package com.akandiah.propmanager.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class JwtConfig {

	/** JWT claim holding global roles; {@code roles: ["ADMIN"]} becomes {@code ROLE_ADMIN}. */
	public static final String ROLES_CLAIM = "roles";

	/**
	 * Converts the {@code roles} claim into Spring authorities.
	 * Logto only emits it when a custom JWT claim is configured; otherwise authorities are empty.
	 */
	@Bean
	JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
		authoritiesConverter.setAuthoritiesClaimName(ROLES_CLAIM);
		authoritiesConverter.setAuthorityPrefix("ROLE_");

		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
		return converter;
	}

	/** Logto JwtDecoder — supports ES384 (Logto's algorithm) and at+jwt typ (RFC 9068). */
	@Bean
	@Profile({"prod", "dev-oidc"})
	JwtDecoder jwtDecoder(
			@Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwksUri,
			@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
			@Value("${app.auth.audience:}") String audience,
			Environment environment) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwksUri)
				.jwsAlgorithm(SignatureAlgorithm.ES384)
				.jwtProcessorCustomizer(p -> p.setJWSTypeVerifier(
						new DefaultJOSEObjectTypeVerifier<>(
								new JOSEObjectType("at+jwt"),
								new JOSEObjectType("JWT"),
								JOSEObjectType.JWT,
								null)))
				.build();
		// Explicit validators: the default JwtTypeValidator rejects Logto's "at+jwt" typ.
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
				new JwtIssuerValidator(issuerUri),
				new JwtTimestampValidator(),
				audienceValidatorFor(audience, environment)));
		return decoder;
	}

	/** Audience is mandatory in prod; dev-oidc may skip it so local setups keep working. */
	private static OAuth2TokenValidator<Jwt> audienceValidatorFor(String audience, Environment environment) {
		if (!audience.isBlank()) {
			return audienceValidator(audience);
		}
		if (Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
			throw new IllegalStateException("AUTH_AUDIENCE must be set in prod (the Logto API resource identifier)");
		}
		log.warn("AUTH_AUDIENCE is not set — JWT audience check is DISABLED. Tokens issued for other apps will be accepted.");
		return jwt -> OAuth2TokenValidatorResult.success();
	}

	/** Rejects tokens not issued for this API, so tokens from other apps on the same Logto are refused. */
	static OAuth2TokenValidator<Jwt> audienceValidator(String audience) {
		OAuth2Error error = new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN,
				"Token audience does not include " + audience, null);
		return jwt -> jwt.getAudience() != null && jwt.getAudience().contains(audience)
				? OAuth2TokenValidatorResult.success()
				: OAuth2TokenValidatorResult.failure(error);
	}
}
