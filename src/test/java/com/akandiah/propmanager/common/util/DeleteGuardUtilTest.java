package com.akandiah.propmanager.common.util;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.akandiah.propmanager.common.exception.HasChildrenException;

/**
 * Unit tests for {@link DeleteGuardUtil}.
 * Tests deletion guard logic to prevent orphaning child records.
 */
class DeleteGuardUtilTest {

	@Test
	void shouldAllowDeletionWhenNoChildren() {
		UUID parentId = UUID.randomUUID();

		assertThatCode(() -> DeleteGuardUtil.requireNoChildren(
				"Prop", parentId, 0L, "unit(s)", "Delete those first."))
				.doesNotThrowAnyException();
	}

	@Test
	void shouldThrowHasChildrenExceptionWhenChildrenExist() {
		UUID parentId = UUID.randomUUID();

		assertThatThrownBy(() -> DeleteGuardUtil.requireNoChildren(
				"Prop", parentId, 3L, "unit(s)", "Delete those first."))
				.isInstanceOf(HasChildrenException.class)
				.hasMessageContaining("Cannot delete Prop")
				.hasMessageContaining(parentId.toString())
				.hasMessageContaining("it has 3 unit(s)")
				.hasMessageContaining("Delete those first.");
	}

	@Test
	void shouldThrowWhenSingleChildExists() {
		UUID parentId = UUID.randomUUID();

		assertThatThrownBy(() -> DeleteGuardUtil.requireNoChildren(
				"Unit", parentId, 1L, "lease(s)", "Remove those first."))
				.isInstanceOf(HasChildrenException.class)
				.hasMessageContaining("it has 1 lease(s)");
	}

	@Test
	void shouldThrowWhenManyChildrenExist() {
		UUID parentId = UUID.randomUUID();

		assertThatThrownBy(() -> DeleteGuardUtil.requireNoChildren(
				"Prop", parentId, 42L, "asset(s)", "Delete those first."))
				.isInstanceOf(HasChildrenException.class)
				.hasMessageContaining("it has 42 asset(s)");
	}

	@Test
	void shouldIncludeCustomActionMessageInException() {
		UUID parentId = UUID.randomUUID();

		assertThatThrownBy(() -> DeleteGuardUtil.requireNoChildren(
				"Template", parentId, 5L, "instance(s)", "Archive them instead."))
				.isInstanceOf(HasChildrenException.class)
				.hasMessageContaining("Archive them instead.");
	}

	@Test
	void shouldExposeExceptionProperties() {
		UUID parentId = UUID.randomUUID();

		try {
			DeleteGuardUtil.requireNoChildren(
					"Prop", parentId, 7L, "unit(s)", "Delete those first.");
		} catch (HasChildrenException e) {
			assertThatCode(() -> {
				assert e.getParentName().equals("Prop");
				assert e.getParentId().equals(parentId);
				assert e.getChildCount() == 7L;
				assert e.getChildLabel().equals("unit(s)");
				assert e.getAction().equals("Delete those first.");
			}).doesNotThrowAnyException();
		}
	}

	@Test
	void shouldPreventInstantiation() {
		assertThatThrownBy(() -> {
			var constructor = DeleteGuardUtil.class.getDeclaredConstructor();
			constructor.setAccessible(true);
			constructor.newInstance();
		})
				.hasCauseInstanceOf(UnsupportedOperationException.class)
				.hasRootCauseMessage("Utility class");
	}
}
