package com.akandiah.propmanager.security;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.akandiah.propmanager.common.permission.AccessEntry;
import com.akandiah.propmanager.common.permission.Actions;
import com.akandiah.propmanager.common.permission.PermissionDomains;
import com.akandiah.propmanager.common.permission.ResourceType;
import com.akandiah.propmanager.common.util.SecurityUtils;
import com.akandiah.propmanager.features.lease.domain.LeaseTenantRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public class PermissionGuard {

	private final HierarchyAwareAuthorizationService authorizationService;
	private final LeaseTenantRepository leaseTenantRepository;

	/** Mapping of String names to bitmask values. Used by SpEL overloads. */
	public static final Map<String, Integer> ACTION_MAP = Map.of(
			"READ", Actions.READ,
			"CREATE", Actions.CREATE,
			"UPDATE", Actions.UPDATE,
			"DELETE", Actions.DELETE);

	/** Mapping of String names to domain keys. Used by SpEL overloads. */
	public static final Map<String, String> DOMAIN_MAP = Map.of(
			"LEASES", PermissionDomains.LEASES,
			"MAINTENANCE", PermissionDomains.MAINTENANCE,
			"FINANCES", PermissionDomains.FINANCES,
			"PEOPLE", PermissionDomains.PEOPLE,
			"ORG", PermissionDomains.ORGANIZATION,
			"PORTFOLIO", PermissionDomains.PORTFOLIO);

	/**
	 * Resolves a string action to its bitmask value. Throws if invalid.
	 * 
	 * @throws IllegalArgumentException if action is not in ACTION_MAP
	 */
	private int resolveAction(String action) {
		Integer bit = ACTION_MAP.get(action.toUpperCase());
		if (bit == null) {
			log.error("[PermissionGuard] Invalid Action string in SpEL: {}. This is a developer error.", action);
			throw new IllegalArgumentException("Invalid security action: " + action);
		}
		return bit;
	}

	/**
	 * Resolves a string domain to its key value. Throws if invalid.
	 * 
	 * @throws IllegalArgumentException if domain is not in DOMAIN_MAP
	 */
	private String resolveDomain(String domain) {
		String key = DOMAIN_MAP.get(domain.toUpperCase());
		if (key == null) {
			log.error("[PermissionGuard] Invalid Domain string in SpEL: {}. This is a developer error.", domain);
			throw new IllegalArgumentException("Invalid security domain: " + domain);
		}
		return key;
	}

	/**
	 * String-based overload for SpEL convenience.
	 *
	 * @param action       "READ", "CREATE", "UPDATE", "DELETE"
	 * @param domain       "PORTFOLIO", "LEASES", "MAINTENANCE", etc.
	 * @param resourceType "ORG", "PROPERTY", "UNIT", "ASSET"
	 * @param resourceId   the specific ID of the resource
	 * @param orgId        the organization context
	 */
	public boolean hasAccess(String action, String domain, String resourceType,
			UUID resourceId, UUID orgId) {
		try {
			ResourceType type = ResourceType.valueOf(resourceType.toUpperCase());
			return hasAccess(resolveAction(action), resolveDomain(domain), type, resourceId, orgId);
		} catch (IllegalArgumentException e) {
			if (e.getMessage().contains("security action") || e.getMessage().contains("security domain"))
				throw e; // rethrow from resolve helpers
			log.error("[PermissionGuard] Invalid ResourceType string in SpEL: {}. This is a developer error.", resourceType);
			throw new IllegalArgumentException("Invalid security resource type: " + resourceType);
		}
	}

	/**
	 * Main entry point for hierarchy-aware permission checks.
	 * TENANT is handled via multi-unit anyMatch before hitting the resolver.
	 */
	public boolean hasAccess(int requiredAction, String domain, ResourceType resourceType,
			UUID resourceId, UUID orgId) {
		if (SecurityUtils.isGlobalAdmin()) {
			return true;
		}
		if (resourceType == ResourceType.TENANT) {
			return hasTenantAccessInternal(requiredAction, domain, resourceId, orgId);
		}
		List<AccessEntry> access = SecurityUtils.getAccessFromRequest();
		return authorizationService.allow(access, requiredAction, domain, resourceType, resourceId, orgId);
	}

	/** Resolves the tenant's active unit IDs and checks UNIT-level access on any of them. */
	private boolean hasTenantAccessInternal(int requiredAction, String domain, UUID tenantId, UUID orgId) {
		List<AccessEntry> access = SecurityUtils.getAccessFromRequest();
		return leaseTenantRepository.findUnitIdsByTenantId(tenantId).stream()
				.anyMatch(uid -> authorizationService.allow(
						access, requiredAction, domain, ResourceType.UNIT, uid, orgId));
	}

	/**
	 * String-based overload for ORG-scoped permission checks.
	 */
	public boolean hasOrgAccess(String action, String domain, UUID orgId) {
		return hasOrgAccess(resolveAction(action), resolveDomain(domain), orgId);
	}

	/**
	 * Convenience method for ORG-scoped permission checks.
	 */
	public boolean hasOrgAccess(int action, String domain, UUID orgId) {
		return hasAccess(action, domain, ResourceType.ORG, orgId, orgId);
	}

	/**
	 * String-based overload for asset permission checks.
	 */
	public boolean hasAssetAccess(String action, String domain, UUID assetId, UUID orgId) {
		return hasAssetAccess(resolveAction(action), resolveDomain(domain), assetId, orgId);
	}

	/**
	 * Checks access for an asset resource.
	 */
	public boolean hasAssetAccess(int action, String domain, UUID assetId, UUID orgId) {
		return hasAccess(action, domain, ResourceType.ASSET, assetId, orgId);
	}

	/**
	 * String-based overload for asset creation checks.
	 */
	public boolean hasAssetCreateAccess(String action, String domain,
			UUID propertyId, UUID unitId, UUID orgId) {
		return hasAssetCreateAccess(resolveAction(action), resolveDomain(domain), propertyId, unitId, orgId);
	}

	/**
	 * For asset creation: checks access on the parent resource (property or unit).
	 */
	public boolean hasAssetCreateAccess(int action, String domain,
			UUID propertyId, UUID unitId, UUID orgId) {
		if (propertyId != null) {
			return hasAccess(action, domain, ResourceType.PROPERTY, propertyId, orgId);
		}
		return hasAccess(action, domain, ResourceType.UNIT, unitId, orgId);
	}
}