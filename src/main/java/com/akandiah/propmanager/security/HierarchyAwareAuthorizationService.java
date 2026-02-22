package com.akandiah.propmanager.security;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.akandiah.propmanager.common.permission.AccessEntry;
import com.akandiah.propmanager.common.permission.HierarchyResolver;
import com.akandiah.propmanager.common.permission.PermissionMaskUtil;
import com.akandiah.propmanager.common.permission.ResourceType;
import com.akandiah.propmanager.common.permission.ScopeLevel;

import lombok.RequiredArgsConstructor;

/**
 * Hierarchy-aware authorization: given (action, domain, resourceType, resourceId, orgId),
 * resolves the scope chain and checks the JWT access list via {@link PermissionMaskUtil#hasAccess}.
 * First scope that grants the required action wins.
 */
@Service
@RequiredArgsConstructor
public class HierarchyAwareAuthorizationService {

	private final HierarchyResolver hierarchyResolver;

	/**
	 * Checks whether the user's access list grants the required action for the given
	 * resource in the given domain.
	 *
	 * @param access        hydrated access list (from JWT or request attribute)
	 * @param requiredAction action bit(s), e.g. {@link com.akandiah.propmanager.common.permission.Actions#READ}
	 * @param domain        domain key, e.g. {@link com.akandiah.propmanager.common.permission.PermissionDomains#LEASES}
	 * @param resourceType  ORG, PROPERTY, or UNIT
	 * @param resourceId    id of the resource (for ORG, use orgId)
	 * @param orgId        organization context
	 * @return true if at least one scope in the hierarchy grants the action
	 */
	public boolean allow(List<AccessEntry> access, int requiredAction, String domain,
			ResourceType resourceType, UUID resourceId, UUID orgId) {
		if (access == null || access.isEmpty()) {
			return false;
		}

		List<ScopeLevel> chain = hierarchyResolver.resolve(resourceType, resourceId, orgId);
		for (ScopeLevel level : chain) {
			Integer mask = findMask(access, orgId, level, domain);
			if (mask != null && PermissionMaskUtil.hasAccess(mask, requiredAction)) {
				return true;
			}
		}
		return false;
	}

	private Integer findMask(List<AccessEntry> access, UUID orgId, ScopeLevel level, String domain) {
		for (AccessEntry entry : access) {
			if (entry.orgId().equals(orgId)
					&& level.scopeType().equals(entry.scopeType())
					&& level.scopeId().equals(entry.scopeId())) {
				return entry.permissions().get(domain);
			}
		}
		return null;
	}
}
