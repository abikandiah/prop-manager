package com.akandiah.propmanager.common.permission;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utilities for building per-resource-type access filters from a hydrated access list.
 * Used by list endpoints to return only rows the caller is authorized to see.
 */
public final class AccessListUtil {

	private AccessListUtil() {}

	// ──────────────────────── Filter records ────────────────────────

	public record PropAccessFilter(Set<UUID> orgIds, Set<UUID> propIds) {
		public boolean isEmpty() {
			return orgIds.isEmpty() && propIds.isEmpty();
		}
	}

	public record UnitAccessFilter(Set<UUID> orgIds, Set<UUID> propIds, Set<UUID> unitIds) {
		public boolean isEmpty() {
			return orgIds.isEmpty() && propIds.isEmpty() && unitIds.isEmpty();
		}
	}

	public record LeaseAccessFilter(Set<UUID> orgIds, Set<UUID> propIds, Set<UUID> unitIds) {
		public boolean isEmpty() {
			return orgIds.isEmpty() && propIds.isEmpty() && unitIds.isEmpty();
		}
	}

	// ──────────────────────── Builder helpers ────────────────────────

	/**
	 * Builds a filter for the property list endpoint.
	 * ORG entries grant access to all properties in the org.
	 * PROPERTY entries grant access to that specific property.
	 * UNIT entries do not grant property-list access.
	 */
	public static PropAccessFilter forProps(List<AccessEntry> access, String domain, int action) {
		Set<UUID> orgIds = new HashSet<>();
		Set<UUID> propIds = new HashSet<>();
		for (AccessEntry e : access) {
			int mask = e.permissions().getOrDefault(domain, 0);
			if (!PermissionMaskUtil.hasAccess(mask, action)) continue;
			switch (e.scopeType()) {
				case "ORG" -> orgIds.add(e.orgId());
				case "PROPERTY" -> propIds.add(e.scopeId());
				// UNIT entries do not grant property-list access
			}
		}
		return new PropAccessFilter(orgIds, propIds);
	}

	/**
	 * Builds a filter for the unit list endpoint.
	 * ORG entries grant access to all units in the org (via their property).
	 * PROPERTY entries grant access to all units in that property.
	 * UNIT entries grant access to that specific unit.
	 */
	public static UnitAccessFilter forUnits(List<AccessEntry> access, String domain, int action) {
		Set<UUID> orgIds = new HashSet<>();
		Set<UUID> propIds = new HashSet<>();
		Set<UUID> unitIds = new HashSet<>();
		for (AccessEntry e : access) {
			int mask = e.permissions().getOrDefault(domain, 0);
			if (!PermissionMaskUtil.hasAccess(mask, action)) continue;
			switch (e.scopeType()) {
				case "ORG" -> orgIds.add(e.orgId());
				case "PROPERTY" -> propIds.add(e.scopeId());
				case "UNIT" -> unitIds.add(e.scopeId());
			}
		}
		return new UnitAccessFilter(orgIds, propIds, unitIds);
	}

	/**
	 * Builds a filter for the lease list endpoint.
	 * ORG entries grant access to all leases in the org.
	 * PROPERTY entries grant access to all leases for units in that property.
	 * UNIT entries grant access to all leases for that specific unit.
	 */
	public static LeaseAccessFilter forLeases(List<AccessEntry> access, String domain, int action) {
		Set<UUID> orgIds = new HashSet<>();
		Set<UUID> propIds = new HashSet<>();
		Set<UUID> unitIds = new HashSet<>();
		for (AccessEntry e : access) {
			int mask = e.permissions().getOrDefault(domain, 0);
			if (!PermissionMaskUtil.hasAccess(mask, action)) continue;
			switch (e.scopeType()) {
				case "ORG" -> orgIds.add(e.orgId());
				case "PROPERTY" -> propIds.add(e.scopeId());
				case "UNIT" -> unitIds.add(e.scopeId());
			}
		}
		return new LeaseAccessFilter(orgIds, propIds, unitIds);
	}

	// ──────────────────────── Request helper ────────────────────────

	/**
	 * Extracts the hydrated access list from the current request attribute.
	 * Returns an empty list if the attribute is absent (e.g. in tests without the filter).
	 */
	@SuppressWarnings("unchecked")
	public static List<AccessEntry> fromRequest(HttpServletRequest request) {
		Object attr = request.getAttribute("com.akandiah.propmanager.jwt.access");
		return attr instanceof List<?> list ? (List<AccessEntry>) list : List.of();
	}
}
