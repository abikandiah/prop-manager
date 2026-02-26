package com.akandiah.propmanager.common.util;

import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.akandiah.propmanager.common.permission.AccessEntry;
import com.akandiah.propmanager.security.JwtAccessHydrationFilter;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Static helpers for reading the current authentication and request context.
 */
public final class SecurityUtils {

	private SecurityUtils() {
		throw new UnsupportedOperationException("Utility class");
	}

	public static boolean isGlobalAdmin() {
		var auth = SecurityContextHolder.getContext().getAuthentication();
		return auth != null && auth.getAuthorities().stream()
				.anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
	}

	/**
	 * Retrieves the {@link AccessEntry} list hydrated by
	 * {@link JwtAccessHydrationFilter}
	 * from the current HTTP request attributes. Returns an empty list if there is
	 * no
	 * active request or the attribute is absent.
	 */
	@SuppressWarnings("unchecked")
	public static List<AccessEntry> getAccessFromRequest() {
		ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		if (attrs == null) {
			return List.of();
		}
		HttpServletRequest request = attrs.getRequest();
		Object attr = request.getAttribute(JwtAccessHydrationFilter.REQUEST_ATTRIBUTE_ACCESS);
		return attr instanceof List<?> list ? (List<AccessEntry>) list : List.of();
	}
}
