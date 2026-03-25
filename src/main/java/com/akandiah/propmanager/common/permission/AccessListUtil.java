package com.akandiah.propmanager.common.permission;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import com.akandiah.propmanager.security.JwtAccessHydrationFilter;

/**
 * Builds per-resource-type access filters from a hydrated access list for list
 * endpoints.
 */
public final class AccessListUtil {

	private AccessListUtil() {
	}

	// ──────────────────────── Filter records ────────────────────────

	public record PropAccessFilter(Set<UUID> orgIds, Set<UUID> propIds) {
		public boolean isEmpty() {
			return orgIds.isEmpty() && propIds.isEmpty();
		}
	}

	/**
	 * Filter for resources scoped at org, property, or unit level (units, leases,
	 * etc.).
	 */
	public record ScopedAccessFilter(Set<UUID> orgIds, Set<UUID> propIds, Set<UUID> unitIds) {
		public boolean isEmpty() {
			return orgIds.isEmpty() && propIds.isEmpty() && unitIds.isEmpty();
		}
	}

	// ──────────────────────── Builder helpers ────────────────────────

	public static PropAccessFilter forProps(List<AccessEntry> access, String domain, int action) {
		Set<UUID> orgIds = new HashSet<>();
		Set<UUID> propIds = new HashSet<>();
		for (AccessEntry e : access) {
			int mask = e.permissions().getOrDefault(domain, 0);
			if (!PermissionMaskUtil.hasAccess(mask, action))
				continue;
			switch (e.scopeType()) {
				case ORG -> orgIds.add(e.orgId());
				case PROPERTY -> propIds.add(e.scopeId());
				case UNIT, ASSET -> {} // unit/asset-scoped entries don't broaden prop-level filter
				case LEASE, TENANT -> throw new IllegalStateException(
						"Unexpected scope type in AccessEntry: " + e.scopeType());
			}
		}
		return new PropAccessFilter(orgIds, propIds);
	}

	/** Builds a filter collecting org/property/unit IDs from the access list. */
	public static ScopedAccessFilter forScopedResources(List<AccessEntry> access, String domain, int action) {
		Set<UUID> orgIds = new HashSet<>();
		Set<UUID> propIds = new HashSet<>();
		Set<UUID> unitIds = new HashSet<>();
		for (AccessEntry e : access) {
			int mask = e.permissions().getOrDefault(domain, 0);
			if (!PermissionMaskUtil.hasAccess(mask, action))
				continue;
			switch (e.scopeType()) {
				case ORG -> orgIds.add(e.orgId());
				case PROPERTY -> propIds.add(e.scopeId());
				case UNIT -> unitIds.add(e.scopeId());
				case ASSET -> {} // asset entries carry no broader scope for filtering
				case LEASE, TENANT -> throw new IllegalStateException(
						"Unexpected scope type in AccessEntry: " + e.scopeType());
			}
		}
		return new ScopedAccessFilter(orgIds, propIds, unitIds);
	}

	// ──────────────────────── Scope narrowing ────────────────────────

	/** Restricts a prop filter to a single org (for multi-org users). */
	public static PropAccessFilter narrowToOrg(PropAccessFilter filter, UUID orgId) {
		Set<UUID> orgIds = filter.orgIds().contains(orgId) ? Set.of(orgId) : Set.of();
		return new PropAccessFilter(orgIds, filter.propIds());
	}

	/** Restricts a scoped filter to a single org (for multi-org users). */
	public static ScopedAccessFilter narrowToOrg(ScopedAccessFilter filter, UUID orgId) {
		Set<UUID> orgIds = filter.orgIds().contains(orgId) ? Set.of(orgId) : Set.of();
		return new ScopedAccessFilter(orgIds, filter.propIds(), filter.unitIds());
	}

	// ──────────────────────── Request helper ────────────────────────

	@SuppressWarnings("unchecked")
	public static List<AccessEntry> fromRequest(HttpServletRequest request) {
		Object attr = request.getAttribute(JwtAccessHydrationFilter.REQUEST_ATTRIBUTE_ACCESS);
		return attr instanceof List<?> list ? (List<AccessEntry>) list : List.of();
	}
}
