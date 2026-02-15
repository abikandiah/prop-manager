package com.akandiah.propmanager.common.util;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import jakarta.persistence.OptimisticLockException;

/**
 * Unit tests for {@link OptimisticLockingUtil}.
 * Tests optimistic locking version validation logic.
 */
class OptimisticLockingUtilTest {

	@Test
	void shouldAllowWhenVersionsMatch() {
		UUID entityId = UUID.randomUUID();
		Integer currentVersion = 5;
		Integer clientVersion = 5;

		assertThatCode(() -> OptimisticLockingUtil.requireVersionMatch(
				"Prop", entityId, currentVersion, clientVersion))
				.doesNotThrowAnyException();
	}

	@Test
	void shouldThrowOptimisticLockExceptionWhenVersionMismatch() {
		UUID entityId = UUID.randomUUID();
		Integer currentVersion = 5;
		Integer clientVersion = 3;

		assertThatThrownBy(() -> OptimisticLockingUtil.requireVersionMatch(
				"Prop", entityId, currentVersion, clientVersion))
				.isInstanceOf(OptimisticLockException.class)
				.hasMessageContaining("Prop")
				.hasMessageContaining(entityId.toString())
				.hasMessageContaining("Expected version 3")
				.hasMessageContaining("current version is 5")
				.hasMessageContaining("modified by another user");
	}

	@Test
	void shouldThrowWhenClientVersionIsNewer() {
		UUID entityId = UUID.randomUUID();
		Integer currentVersion = 2;
		Integer clientVersion = 5;

		assertThatThrownBy(() -> OptimisticLockingUtil.requireVersionMatch(
				"Lease", entityId, currentVersion, clientVersion))
				.isInstanceOf(OptimisticLockException.class)
				.hasMessageContaining("Lease")
				.hasMessageContaining("Expected version 5")
				.hasMessageContaining("current version is 2");
	}

	@Test
	void shouldWorkWithVersionZero() {
		UUID entityId = UUID.randomUUID();
		Integer currentVersion = 0;
		Integer clientVersion = 0;

		assertThatCode(() -> OptimisticLockingUtil.requireVersionMatch(
				"Asset", entityId, currentVersion, clientVersion))
				.doesNotThrowAnyException();
	}

	@Test
	void shouldIncludeEntityNameInExceptionMessage() {
		UUID entityId = UUID.randomUUID();

		assertThatThrownBy(() -> OptimisticLockingUtil.requireVersionMatch(
				"CustomEntity", entityId, 10, 8))
				.isInstanceOf(OptimisticLockException.class)
				.hasMessageContaining("CustomEntity");
	}

	@Test
	void shouldPreventInstantiation() {
		assertThatThrownBy(() -> {
			var constructor = OptimisticLockingUtil.class.getDeclaredConstructor();
			constructor.setAccessible(true);
			constructor.newInstance();
		})
				.hasCauseInstanceOf(UnsupportedOperationException.class)
				.hasRootCauseMessage("Utility class");
	}
}
