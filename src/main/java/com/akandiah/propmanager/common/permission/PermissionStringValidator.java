package com.akandiah.propmanager.common.permission;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.akandiah.propmanager.common.exception.InvalidPermissionStringException;
import com.akandiah.propmanager.common.exception.InvalidPermissionStringException.PermissionValidationError;

/** Validates a permissions map (domain key → action letters) against allowed keys and letters. */
public final class PermissionStringValidator {

	private PermissionStringValidator() {
		throw new UnsupportedOperationException("Utility class");
	}

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

	/** Validates and throws {@link InvalidPermissionStringException} with field-level errors if invalid. */
	public static void validate(Map<String, String> permissions) {
		if (permissions == null || permissions.isEmpty()) {
			return;
		}
		List<PermissionValidationError> errors = new ArrayList<>();
		for (Map.Entry<String, String> e : permissions.entrySet()) {
			String key = e.getKey();
			if (!PermissionDomains.VALID_KEYS.contains(key)) {
				errors.add(new PermissionValidationError("permissions." + key,
						"Unknown domain key: '" + key + "'. Allowed: l, m, f, t, o, p"));
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
