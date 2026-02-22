package com.akandiah.propmanager.common.permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.akandiah.propmanager.common.exception.InvalidPermissionStringException;

/**
 * Unit tests for {@link PermissionStringValidator}.
 */
class PermissionStringValidatorTest {

	@Nested
	class IsValid {

		@Test
		void shouldAcceptNull() {
			assertThat(PermissionStringValidator.isValid(null)).isTrue();
		}

		@Test
		void shouldAcceptEmpty() {
			assertThat(PermissionStringValidator.isValid(Map.of())).isTrue();
		}

		@Test
		void shouldAcceptValidMaps() {
			assertThat(PermissionStringValidator.isValid(Map.of("l", "r"))).isTrue();
			assertThat(PermissionStringValidator.isValid(Map.of("l", "cru", "m", "r", "f", "crud"))).isTrue();
			assertThat(PermissionStringValidator.isValid(Map.of(PermissionDomains.LEASES, "rc",
					PermissionDomains.MAINTENANCE, "r", PermissionDomains.FINANCES, ""))).isTrue();
		}

		@Test
		void shouldRejectInvalidKey() {
			assertThat(PermissionStringValidator.isValid(Map.of("x", "r"))).isFalse();
			assertThat(PermissionStringValidator.isValid(Map.of("l", "r", "org", "c"))).isFalse();
		}

		@Test
		void shouldRejectInvalidLetter() {
			assertThat(PermissionStringValidator.isValid(Map.of("l", "rx"))).isFalse();
			assertThat(PermissionStringValidator.isValid(Map.of("m", "crux"))).isFalse();
			assertThat(PermissionStringValidator.isValid(Map.of("f", "R"))).isFalse();
		}
	}

	@Nested
	class Validate {

		@Test
		void shouldNotThrowForNull() {
			PermissionStringValidator.validate(null);
		}

		@Test
		void shouldNotThrowForEmpty() {
			PermissionStringValidator.validate(Map.of());
		}

		@Test
		void shouldNotThrowForValidMaps() {
			PermissionStringValidator.validate(Map.of("l", "r"));
			PermissionStringValidator.validate(Map.of("l", "cru", "m", "r", "f", "crud"));
		}

		@Test
		void shouldThrowForInvalidKey() {
			assertThatThrownBy(() -> PermissionStringValidator.validate(Map.of("x", "r")))
					.isInstanceOf(InvalidPermissionStringException.class)
					.satisfies(ex -> {
						InvalidPermissionStringException ie = (InvalidPermissionStringException) ex;
						assertThat(ie.getErrors()).hasSize(1);
						assertThat(ie.getErrors().get(0).field()).isEqualTo("permissions.x");
						assertThat(ie.getErrors().get(0).message()).contains("Unknown domain key");
					});
		}

		@Test
		void shouldThrowForInvalidLetter() {
			assertThatThrownBy(() -> PermissionStringValidator.validate(Map.of("l", "crux")))
					.isInstanceOf(InvalidPermissionStringException.class)
					.satisfies(ex -> {
						InvalidPermissionStringException ie = (InvalidPermissionStringException) ex;
						assertThat(ie.getErrors()).hasSize(1);
						assertThat(ie.getErrors().get(0).field()).isEqualTo("permissions.l");
						assertThat(ie.getErrors().get(0).message()).contains("Invalid action letter");
					});
		}

		@Test
		void shouldReportMultipleErrors() {
			assertThatThrownBy(() -> PermissionStringValidator.validate(Map.of("x", "r", "m", "rq")))
					.isInstanceOf(InvalidPermissionStringException.class)
					.satisfies(ex -> {
						InvalidPermissionStringException ie = (InvalidPermissionStringException) ex;
						assertThat(ie.getErrors()).hasSize(2);
						assertThat(ie.getErrors()).extracting(e -> e.field())
								.containsExactlyInAnyOrder("permissions.x", "permissions.m");
					});
		}
	}
}
