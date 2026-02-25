package com.akandiah.propmanager.common.util;

import org.springframework.security.core.context.SecurityContextHolder;

/** Static helpers for reading the current authentication context. */
public final class SecurityUtils {

	private SecurityUtils() {
		throw new UnsupportedOperationException("Utility class");
	}

	public static boolean isGlobalAdmin() {
		var auth = SecurityContextHolder.getContext().getAuthentication();
		return auth != null && auth.getAuthorities().stream()
				.anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
	}
}
