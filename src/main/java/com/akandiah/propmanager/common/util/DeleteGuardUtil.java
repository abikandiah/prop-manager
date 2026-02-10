package com.akandiah.propmanager.common.util;

import java.util.UUID;

import com.akandiah.propmanager.common.exception.HasChildrenException;

/**
 * Utility for guarding deletes when an entity has child records that must be removed first.
 */
public final class DeleteGuardUtil {

	private DeleteGuardUtil() {
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Throws {@link HasChildrenException} if {@code childCount > 0}.
	 *
	 * @param parentName  e.g. "Prop", "Unit", "Lease"
	 * @param parentId   the entity id
	 * @param childCount number of child records
	 * @param childLabel e.g. "unit(s)", "asset(s)", "tenant assignment(s)"
	 * @param action     e.g. "Delete those first." or "Remove those first."
	 */
	public static void requireNoChildren(String parentName, UUID parentId, long childCount,
			String childLabel, String action) {
		if (childCount > 0) {
			throw new HasChildrenException(parentName, parentId, childCount, childLabel, action);
		}
	}
}
