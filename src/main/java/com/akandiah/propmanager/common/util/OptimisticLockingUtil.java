package com.akandiah.propmanager.common.util;

import jakarta.persistence.OptimisticLockException;

/**
 * Utility for handling optimistic locking checks across entities.
 */
public final class OptimisticLockingUtil {

	private OptimisticLockingUtil() {
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Validates that the client's version matches the entity's current version.
	 *
	 * @param entityName    the name of the entity (e.g., "Prop", "Lease")
	 * @param entityId      the entity's ID for error messaging
	 * @param currentVersion the entity's current version
	 * @param clientVersion  the version provided by the client
	 * @throws OptimisticLockException if versions don't match
	 */
	public static void requireVersionMatch(String entityName, Object entityId,
			Integer currentVersion, Integer clientVersion) {
		if (!currentVersion.equals(clientVersion)) {
			throw new OptimisticLockException(
					entityName + " " + entityId + " has been modified by another user. "
							+ "Expected version " + clientVersion
							+ " but current version is " + currentVersion);
		}
	}
}
