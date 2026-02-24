package com.akandiah.propmanager.common.permission;

import java.util.List;
import java.util.UUID;

/** Resolves the hierarchy chain (most specific → least specific) for a resource. */
public interface HierarchyResolver {

	List<ScopeLevel> resolve(ResourceType resourceType, UUID resourceId, UUID orgId);
}
