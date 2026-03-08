package com.akandiah.propmanager.common.permission;

/**
 * Resource types for hierarchy-aware authorization.
 * ORG/PROPERTY/UNIT/ASSET are scope levels (appear in AccessEntry chains).
 * LEASE flows through the resolver and produces a UNIT-level chain.
 * TENANT is intercepted by PermissionGuard (multi-unit anyMatch) and never reaches the resolver.
 */
public enum ResourceType {
	ORG,
	PROPERTY,
	UNIT,
	ASSET,
	LEASE,
	TENANT
}
