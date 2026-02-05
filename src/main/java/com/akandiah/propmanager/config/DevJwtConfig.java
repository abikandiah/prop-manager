package com.akandiah.propmanager.config;

import java.security.SecureRandom;
import java.util.HexFormat;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Profile("dev")
@Slf4j
public class DevJwtConfig {

	@Value("${app.jwt.secret}")
	private String jwtSecret;

	@Getter
	private String devLoginSecret;

	@PostConstruct
	public void init() {
		byte[] bytes = new byte[16];
		new SecureRandom().nextBytes(bytes);
		this.devLoginSecret = HexFormat.of().formatHex(bytes);
		log.info("\n" + "=".repeat(60) + "\n" + "DEV LOGIN SECRET (HEX): {}\n" + "=".repeat(60), devLoginSecret);
	}

	/**
	 * Provides a local JwtDecoder that uses a symmetric key (HMAC).
	 * This allows local testing without an external OIDC provider.
	 */
	@Bean
	public JwtDecoder jwtDecoder() {
		SecretKeySpec secretKey = new SecretKeySpec(jwtSecret.getBytes(), "HmacSHA256");
		return NimbusJwtDecoder.withSecretKey(secretKey).build();
	}
}
