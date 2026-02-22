package com.akandiah.propmanager.common.permission;

/**
 * Converts permission strings (e.g. "cru") to/from bitmasks for JWT and authorization checks.
 */
public final class PermissionMaskUtil {

	private PermissionMaskUtil() {
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Parses a string of action letters into a single bitmask. Order-independent; invalid
	 * characters are ignored.
	 *
	 * @param letters e.g. "cru" or "ruc" → create + read + update
	 * @return combined mask (0 if null or empty)
	 */
	public static int parseToMask(String letters) {
		if (letters == null || letters.isEmpty()) {
			return 0;
		}
		int mask = 0;
		for (int i = 0; i < letters.length(); i++) {
			int bit = Actions.fromLetter(letters.charAt(i));
			if (bit != 0) {
				mask |= bit;
			}
		}
		return mask;
	}

	/**
	 * Converts a bitmask back to a string of letters (r, c, u, d) for display or audit.
	 * Order is fixed: r, c, u, d.
	 *
	 * @param mask combined permission mask
	 * @return e.g. "cru" or "" if no bits set
	 */
	public static String maskToLetters(int mask) {
		if (mask == 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder(4);
		if ((mask & Actions.READ) != 0) {
			sb.append('r');
		}
		if ((mask & Actions.CREATE) != 0) {
			sb.append('c');
		}
		if ((mask & Actions.UPDATE) != 0) {
			sb.append('u');
		}
		if ((mask & Actions.DELETE) != 0) {
			sb.append('d');
		}
		return sb.toString();
	}

	/**
	 * Checks whether the user's mask includes the required action (and possibly more).
	 *
	 * @param userMask       the permission mask from the JWT or DB
	 * @param requiredAction one or more actions (e.g. Actions.UPDATE or Actions.READ | Actions.UPDATE)
	 * @return true if all bits in requiredAction are set in userMask
	 */
	public static boolean hasAccess(int userMask, int requiredAction) {
		return (userMask & requiredAction) == requiredAction;
	}
}
