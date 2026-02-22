package com.akandiah.propmanager.common.permission;

import java.util.List;
import java.util.UUID;

/**
 * Resolves the hierarchy chain (most specific to least) for a resource.
 * Used by the authorization service to find the first matching scope that grants access.
 */
public interface HierarchyResolver {

	/**
	 * Resolves the scope chain for the given resource. The list is ordered from
	 * most specific (e.g. UNIT) to least specific (ORG).
	 *
	 * @param resourceType type of resource (ORG, PROPERTY, UNIT)
	 * @param resourceId   id of the resource (for ORG, typically same as orgId)
	 * @param orgId        organization context; resource must belong to this org
	 * @return ordered list of scope levels, or empty if resource not found or not in org
	 */
	List<ScopeLevel> resolve(ResourceType resourceType, UUID resourceId, UUID orgId);
}
