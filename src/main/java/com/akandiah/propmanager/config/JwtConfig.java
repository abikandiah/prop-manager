package com.akandiah.propmanager.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

@Configuration
public class JwtConfig {

	/**
	 * PRODUCTION: Only active when 'prod' profile is set.
	 * Connects to Authentik for Public Keys.
	 */
	@Bean
	@Profile("prod")
	JwtDecoder jwtDecoderProd(
			@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri) {
		return JwtDecoders.fromIssuerLocation(issuerUri);
	}

	/**
	 * DEVELOPMENT: Only active when 'dev' profile is set.
	 * Uses local Secret Key for offline testing.
	 */
	@Bean
	@Profile("dev")
	JwtDecoder jwtDecoderDev(JwtProperties properties) {
		SecretKey key = new SecretKeySpec(
				properties.secret().getBytes(StandardCharsets.UTF_8),
				"HmacSHA256");
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).build();
		decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
		return decoder;
	}

	/**
	 * SHARED: Converts JWT claims (like 'groups') into Spring Authorities.
	 */
	@Bean
	JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();

		// Matches Authentik's default 'groups' claim
		authoritiesConverter.setAuthoritiesClaimName("groups");
		// Adds 'ROLE_' so @PreAuthorize("hasRole('ADMIN')") works
		authoritiesConverter.setAuthorityPrefix("ROLE_");

		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
		return converter;
	}
}