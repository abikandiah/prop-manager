package com.akandiah.propmanager.common.permission;

import java.util.Set;

/** Bitmask values for permission actions. Each action is a power of 2 for bitwise combination. */
public final class Actions {

	private Actions() {
		throw new UnsupportedOperationException("Constants class");
	}

	public static final int READ = 1;
	public static final int CREATE = 2;
	public static final int UPDATE = 4;
	public static final int DELETE = 8;

	public static final Set<Character> VALID_LETTERS = Set.of('r', 'c', 'u', 'd');

	/** Maps a single letter to its action bit. Returns 0 for unknown characters. */
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
