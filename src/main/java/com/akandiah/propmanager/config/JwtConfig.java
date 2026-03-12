package com.akandiah.propmanager.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
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

@Configuration
public class JwtConfig {

	/**
	 * Converts JWT claims into Spring Authorities.
	 * Logto does not use a groups claim — authorities will be empty for all users.
	 */
	@Bean
	JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
		authoritiesConverter.setAuthoritiesClaimName("roles");
		authoritiesConverter.setAuthorityPrefix("ROLE_");

		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
		return converter;
	}

	/** Prod JwtDecoder — supports ES384 (Logto's algorithm) and at+jwt typ (RFC 9068). */
	@Bean
	@Profile("prod")
	JwtDecoder jwtDecoder(
			@Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwksUri,
			@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwksUri)
				.jwsAlgorithm(SignatureAlgorithm.ES384)
				.jwtProcessorCustomizer(p -> p.setJWSTypeVerifier(
						new DefaultJOSEObjectTypeVerifier<>(
								new JOSEObjectType("at+jwt"),
								new JOSEObjectType("JWT"),
								JOSEObjectType.JWT,
								null)))
				.build();
		// Use explicit validators to avoid JwtTypeValidator which only allows "JWT" typ.
		// Logto issues access tokens with typ "at+jwt" (RFC 9068).
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<Jwt>(
				new JwtIssuerValidator(issuerUri),
				new JwtTimestampValidator()));
		return decoder;
	}
}