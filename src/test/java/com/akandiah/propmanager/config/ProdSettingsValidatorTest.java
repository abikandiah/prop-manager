package com.akandiah.propmanager.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ProdSettingsValidatorTest {

	@Test
	void shouldAcceptWhenAllSet() {
		assertThatCode(() -> new ProdSettingsValidator(
				new AppProperties("https://prop-mange.akandiah.ca"), "no-reply@akandiah.ca"))
				.doesNotThrowAnyException();
	}

	@Test
	void shouldRejectMissingBaseUrl() {
		assertThatThrownBy(() -> new ProdSettingsValidator(new AppProperties(""), "no-reply@akandiah.ca"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("APP_BASE_URL");
	}

	@Test
	void shouldRejectMissingMailFrom() {
		assertThatThrownBy(() -> new ProdSettingsValidator(new AppProperties("https://prop-mange.akandiah.ca"), " "))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("MAIL_FROM");
	}
}
