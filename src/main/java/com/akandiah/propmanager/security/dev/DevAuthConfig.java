package com.akandiah.propmanager.security.dev;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import lombok.extern.slf4j.Slf4j;

/**
 * Dev profile only: generates a secure-random dev password on startup and logs
 * it. The UI uses this password with POST /dev/token to obtain a JWT. Only this
 * password is accepted by the dev token endpoint.
 */
@Configuration
@Profile("dev")
@Slf4j
public class DevAuthConfig {

	private static final int DEV_PASSWORD_BYTES = 32;

	@Bean
	String devAuthPassword() {
		SecureRandom random = new SecureRandom();
		byte[] bytes = new byte[DEV_PASSWORD_BYTES];
		random.nextBytes(bytes);
		String password = Base64.getEncoder().withoutPadding().encodeToString(bytes);
		log.info("\n--- DEV AUTH ---\nDev token password (use with POST /dev/token): {}\n----------------\n", password);
		return password;
	}
}
