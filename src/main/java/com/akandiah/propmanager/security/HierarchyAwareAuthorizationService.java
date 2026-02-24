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

/** Resolves the scope chain and checks the JWT access list. First matching scope wins. */
@Service
@RequiredArgsConstructor
public class HierarchyAwareAuthorizationService {

	private final HierarchyResolver hierarchyResolver;

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
					&& level.scopeType() == entry.scopeType()
					&& level.scopeId().equals(entry.scopeId())) {
				return entry.permissions().get(domain);
			}
		}
		return null;
	}
}
