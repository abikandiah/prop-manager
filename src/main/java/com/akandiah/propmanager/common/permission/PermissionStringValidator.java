package com.akandiah.propmanager.common.permission;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.akandiah.propmanager.common.exception.InvalidPermissionStringException;
import com.akandiah.propmanager.common.exception.InvalidPermissionStringException.PermissionValidationError;

/**
 * Validates a permissions map (domain key → action letters) for use in templates,
 * membership, and member_scope. Uses Piece 1 constants: allowed domain keys
 * ({@link PermissionDomains#VALID_KEYS}) and action letters ({@link Actions#VALID_LETTERS}).
 */
public final class PermissionStringValidator {

	private PermissionStringValidator() {
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Checks whether the permissions map is valid (all keys and action letters allowed).
	 *
	 * @param permissions map from domain key (e.g. "l", "m", "f") to action string (e.g. "cru")
	 * @return true if null, empty, or all entries valid
	 */
	public static boolean isValid(Map<String, String> permissions) {
		if (permissions == null || permissions.isEmpty()) {
			return true;
		}
		for (Map.Entry<String, String> e : permissions.entrySet()) {
			if (!PermissionDomains.VALID_KEYS.contains(e.getKey())) {
				return false;
			}
			String letters = e.getValue();
			if (letters != null) {
				for (int i = 0; i < letters.length(); i++) {
					if (!Actions.VALID_LETTERS.contains(letters.charAt(i))) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * Validates the permissions map and throws {@link InvalidPermissionStringException}
	 * with field-level errors if invalid. Use in services so GlobalExceptionHandler
	 * returns 400 with an {@code errors} array.
	 *
	 * @param permissions map from domain key to action letters (e.g. "l" → "cru")
	 * @throws InvalidPermissionStringException if any key or action letter is invalid
	 */
	public static void validate(Map<String, String> permissions) {
		if (permissions == null || permissions.isEmpty()) {
			return;
		}
		List<PermissionValidationError> errors = new ArrayList<>();
		for (Map.Entry<String, String> e : permissions.entrySet()) {
			String key = e.getKey();
			if (!PermissionDomains.VALID_KEYS.contains(key)) {
				errors.add(new PermissionValidationError("permissions." + key,
						"Unknown domain key: '" + key + "'. Allowed: l, m, f"));
				continue;
			}
			String letters = e.getValue();
			if (letters == null) {
				continue;
			}
			for (int i = 0; i < letters.length(); i++) {
				char c = letters.charAt(i);
				if (!Actions.VALID_LETTERS.contains(c)) {
					errors.add(new PermissionValidationError("permissions." + key,
							"Invalid action letter: '" + c + "'. Allowed: r, c, u, d"));
					break;
				}
			}
		}
		if (!errors.isEmpty()) {
			throw new InvalidPermissionStringException(errors);
		}
	}
}
