package com.akandiah.propmanager.security;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.akandiah.propmanager.common.permission.AccessEntry;
import com.akandiah.propmanager.common.permission.ResourceType;
import com.akandiah.propmanager.common.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

/**
 * SpEL bean for {@code @PreAuthorize}: checks granular permission scopes from the JWT access
 * list. Wraps {@link HierarchyAwareAuthorizationService} for controller-level SpEL gates.
 *
 * <p>Admins are always allowed — they bypass the scope check.
 */
@Component("permissionAuth")
@RequiredArgsConstructor
public class PermissionAuthorizationComponent {

	private final HierarchyAwareAuthorizationService authorizationService;

	/**
	 * Convenience method for ORG-scoped permission checks.
	 * Equivalent to {@code hasAccess(action, domain, ORG, orgId, orgId)}.
	 *
	 * @param action bitmask value from {@link com.akandiah.propmanager.common.permission.Actions}
	 *               (READ=1, CREATE=2, UPDATE=4, DELETE=8)
	 * @param domain permission domain key from {@link com.akandiah.propmanager.common.permission.PermissionDomains}
	 * @param orgId  the organization ID (used as both resourceId and orgId for ORG-level checks)
	 */
	public boolean hasOrgAccess(int action, String domain, UUID orgId) {
		if (SecurityUtils.isGlobalAdmin()) {
			return true;
		}
		List<AccessEntry> access = SecurityUtils.getAccessFromRequest();
		return authorizationService.allow(access, action, domain, ResourceType.ORG, orgId, orgId);
	}
}
