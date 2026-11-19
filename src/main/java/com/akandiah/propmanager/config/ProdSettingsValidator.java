package com.akandiah.propmanager.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Fails prod startup when settings that would otherwise send broken emails are missing. */
@Component
@Profile("prod")
class ProdSettingsValidator {

	ProdSettingsValidator(
			AppProperties appProperties,
			@Value("${spring.mail.properties.mail.from:}") String mailFrom) {
		require(appProperties.baseUrl(), "APP_BASE_URL");
		require(mailFrom, "MAIL_FROM");
	}

	private static void require(String value, String envVar) {
		if (value == null || value.isBlank()) {
			throw new IllegalStateException(envVar + " must be set in prod");
		}
	}
}
