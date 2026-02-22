package com.akandiah.propmanager.common.permission;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PermissionMaskUtil}.
 */
class PermissionMaskUtilTest {

	@Nested
	class ParseToMask {

		@Test
		void shouldParseCruToSeven() {
			assertThat(PermissionMaskUtil.parseToMask("cru")).isEqualTo(7); // 1 + 2 + 4
		}

		@Test
		void shouldBeOrderIndependent() {
			assertThat(PermissionMaskUtil.parseToMask("ruc")).isEqualTo(7);
			assertThat(PermissionMaskUtil.parseToMask("cur")).isEqualTo(7);
		}

		@Test
		void shouldParseCrudToFifteen() {
			assertThat(PermissionMaskUtil.parseToMask("crud")).isEqualTo(15);
		}

		@Test
		void shouldParseSingleLetter() {
			assertThat(PermissionMaskUtil.parseToMask("r")).isEqualTo(Actions.READ);
			assertThat(PermissionMaskUtil.parseToMask("c")).isEqualTo(Actions.CREATE);
			assertThat(PermissionMaskUtil.parseToMask("u")).isEqualTo(Actions.UPDATE);
			assertThat(PermissionMaskUtil.parseToMask("d")).isEqualTo(Actions.DELETE);
		}

		@Test
		void shouldIgnoreInvalidCharacters() {
			assertThat(PermissionMaskUtil.parseToMask("crxu")).isEqualTo(7);
			assertThat(PermissionMaskUtil.parseToMask("x")).isEqualTo(0);
		}

		@Test
		void shouldReturnZeroForNullOrEmpty() {
			assertThat(PermissionMaskUtil.parseToMask(null)).isEqualTo(0);
			assertThat(PermissionMaskUtil.parseToMask("")).isEqualTo(0);
		}
	}

	@Nested
	class MaskToLetters {

		@Test
		void shouldConvertSevenToCru() {
			assertThat(PermissionMaskUtil.maskToLetters(7)).isEqualTo("rcu");
		}

		@Test
		void shouldConvertFifteenToCrud() {
			assertThat(PermissionMaskUtil.maskToLetters(15)).isEqualTo("rcud");
		}

		@Test
		void shouldConvertSingleBit() {
			assertThat(PermissionMaskUtil.maskToLetters(Actions.READ)).isEqualTo("r");
			assertThat(PermissionMaskUtil.maskToLetters(Actions.CREATE)).isEqualTo("c");
			assertThat(PermissionMaskUtil.maskToLetters(Actions.UPDATE)).isEqualTo("u");
			assertThat(PermissionMaskUtil.maskToLetters(Actions.DELETE)).isEqualTo("d");
		}

		@Test
		void shouldReturnEmptyForZero() {
			assertThat(PermissionMaskUtil.maskToLetters(0)).isEmpty();
		}

		@Test
		void roundTripMatches() {
			assertThat(PermissionMaskUtil.parseToMask(PermissionMaskUtil.maskToLetters(7))).isEqualTo(7);
			assertThat(PermissionMaskUtil.parseToMask(PermissionMaskUtil.maskToLetters(15))).isEqualTo(15);
		}
	}

	@Nested
	class HasAccess {

		@Test
		void shouldAllowWhenUserHasRequiredAction() {
			int userLeaseMask = 7; // cru
			assertThat(PermissionMaskUtil.hasAccess(userLeaseMask, Actions.READ)).isTrue();
			assertThat(PermissionMaskUtil.hasAccess(userLeaseMask, Actions.UPDATE)).isTrue();
			assertThat(PermissionMaskUtil.hasAccess(userLeaseMask, Actions.READ | Actions.UPDATE)).isTrue();
		}

		@Test
		void shouldDenyWhenUserMissingRequiredAction() {
			int userLeaseMask = 7; // cru, no delete
			assertThat(PermissionMaskUtil.hasAccess(userLeaseMask, Actions.DELETE)).isFalse();
			assertThat(PermissionMaskUtil.hasAccess(userLeaseMask, Actions.READ | Actions.DELETE)).isFalse();
		}

		@Test
		void shouldAllowWhenUserMaskIsSuperset() {
			int fullMask = 15; // crud
			assertThat(PermissionMaskUtil.hasAccess(fullMask, Actions.READ)).isTrue();
			assertThat(PermissionMaskUtil.hasAccess(fullMask, Actions.DELETE)).isTrue();
		}

		@Test
		void shouldDenyWhenUserMaskIsZero() {
			assertThat(PermissionMaskUtil.hasAccess(0, Actions.READ)).isFalse();
			assertThat(PermissionMaskUtil.hasAccess(0, Actions.CREATE)).isFalse();
		}
	}
}
