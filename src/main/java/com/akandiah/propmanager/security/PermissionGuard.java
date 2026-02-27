package com.akandiah.propmanager.security;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.akandiah.propmanager.common.permission.AccessEntry;
import com.akandiah.propmanager.common.permission.ResourceType;
import com.akandiah.propmanager.common.util.SecurityUtils;
import com.akandiah.propmanager.features.lease.domain.LeaseRepository;

import lombok.RequiredArgsConstructor;

/**
 * SpEL bean for {@code @PreAuthorize}: checks granular permission scopes from
 * the hydrated access
 * list. Wraps {@link HierarchyAwareAuthorizationService} for controller-level
 * SpEL gates.
 *
 * <p>
 * Admins (ROLE_ADMIN) are always allowed — they bypass the scope check.
 */
@Component("permissionGuard")
@RequiredArgsConstructor
public class PermissionGuard {

	private final HierarchyAwareAuthorizationService authorizationService;
	private final LeaseRepository leaseRepository;

	/**
	 * Main entry point for hierarchy-aware permission checks.
	 *
	 * @param requiredAction bitmask value (READ=1, CREATE=2, UPDATE=4, DELETE=8)
	 * @param domain         permission domain key (e.g. 'l' for leases/props)
	 * @param resourceType   the type of resource being accessed
	 * @param resourceId     the specific ID of the resource
	 * @param orgId          the organization context
	 */
	public boolean hasAccess(int requiredAction, String domain, ResourceType resourceType,
			UUID resourceId, UUID orgId) {
		if (SecurityUtils.isGlobalAdmin()) {
			return true;
		}
		List<AccessEntry> access = SecurityUtils.getAccessFromRequest();
		return authorizationService.allow(access, requiredAction, domain, resourceType, resourceId, orgId);
	}

	/**
	 * Convenience method for ORG-scoped permission checks.
	 * Equivalent to {@code hasAccess(action, domain, ORG, orgId, orgId)}.
	 */
	public boolean hasOrgAccess(int action, String domain, UUID orgId) {
		return hasAccess(action, domain, ResourceType.ORG, orgId, orgId);
	}

	/**
	 * Resolves the lease's unit, then checks UNIT-level access. Returns false if
	 * lease not found.
	 */
	public boolean hasLeaseAccess(int requiredAction, String domain, UUID leaseId, UUID orgId) {
		return leaseRepository.findUnitIdById(leaseId)
				.map(unitId -> hasAccess(requiredAction, domain, ResourceType.UNIT, unitId, orgId))
				.orElse(false);
	}

	/**
	 * Checks access for an asset resource.
	 */
	public boolean hasAssetAccess(int action, String domain, UUID assetId, UUID orgId) {
		return hasAccess(action, domain, ResourceType.ASSET, assetId, orgId);
	}

	/**
	 * For asset creation: checks access on the parent resource (property or unit).
	 * Exactly one of {@code propertyId} / {@code unitId} must be non-null.
	 */
	public boolean hasAssetCreateAccess(int action, String domain,
			UUID propertyId, UUID unitId, UUID orgId) {
		if (propertyId != null) {
			return hasAccess(action, domain, ResourceType.PROPERTY, propertyId, orgId);
		}
		return hasAccess(action, domain, ResourceType.UNIT, unitId, orgId);
	}
}
