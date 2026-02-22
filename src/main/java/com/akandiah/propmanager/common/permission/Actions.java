package com.akandiah.propmanager.common.permission;

import java.util.Set;

/**
 * Bitmask values for permission actions. Single source of truth for JWT and DB string mapping.
 * Each action is a power of 2 for bitwise combination.
 */
public final class Actions {

	private Actions() {
		throw new UnsupportedOperationException("Constants class");
	}

	/** Read */
	public static final int READ = 1;    // 0001
	/** Create */
	public static final int CREATE = 2;  // 0010
	/** Update */
	public static final int UPDATE = 4;  // 0100
	/** Delete */
	public static final int DELETE = 8;  // 1000

	/** Valid letters for permission strings (e.g. "cru" → create, read, update). */
	public static final Set<Character> VALID_LETTERS = Set.of('r', 'c', 'u', 'd');

	/**
	 * Maps a single letter to its action bit. Returns 0 for unknown characters.
	 *
	 * @param letter one of 'r', 'c', 'u', 'd' (case-sensitive)
	 * @return the action bit, or 0 if not valid
	 */
	public static int fromLetter(char letter) {
		return switch (letter) {
			case 'r' -> READ;
			case 'c' -> CREATE;
			case 'u' -> UPDATE;
			case 'd' -> DELETE;
			default -> 0;
		};
	}
}
