package com.akandiah.propmanager.common.permission;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * One entry in the JWT "access" claim: effective permissions for a scope (org or property/unit).
 * JSON-serializable for JWT and request attributes.
 */
public record AccessEntry(
		UUID orgId,
		String scopeType,
		UUID scopeId,
		Map<String, Integer> permissions) {

	public AccessEntry {
		permissions = permissions != null ? Map.copyOf(permissions) : Map.of();
	}

	/**
	 * Converts this entry to a map suitable for JWT claim (string keys, UUIDs as strings).
	 */
	public Map<String, Object> toClaimMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("orgId", orgId.toString());
		map.put("scopeType", scopeType);
		map.put("scopeId", scopeId.toString());
		map.put("permissions", new LinkedHashMap<>(permissions));
		return map;
	}

	/**
	 * Builds an AccessEntry from a JWT claim map (e.g. from Jwt.getClaim("access")).
	 */
	public static AccessEntry fromClaimMap(Map<String, ?> map) {
		UUID orgId = UUID.fromString(Objects.requireNonNull((String) map.get("orgId")));
		String scopeType = (String) map.get("scopeType");
		UUID scopeId = UUID.fromString(Objects.requireNonNull((String) map.get("scopeId")));
		Map<String, Integer> perms = new LinkedHashMap<>();
		Object p = map.get("permissions");
		if (p instanceof Map<?, ?> m) {
			m.forEach((k, v) -> perms.put(String.valueOf(k), ((Number) v).intValue()));
		}
		return new AccessEntry(orgId, scopeType, scopeId, Collections.unmodifiableMap(perms));
	}

	/**
	 * Builds a list of AccessEntry from a JWT "access" claim (list of maps).
	 */
	@SuppressWarnings("unchecked")
	public static List<AccessEntry> fromClaimList(List<?> list) {
		if (list == null || list.isEmpty()) {
			return List.of();
		}
		return list.stream()
				.filter(m -> m instanceof Map)
				.map(m -> fromClaimMap((Map<String, ?>) m))
				.collect(Collectors.toList());
	}
}
