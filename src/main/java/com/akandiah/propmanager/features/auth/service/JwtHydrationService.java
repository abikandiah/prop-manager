package com.akandiah.propmanager.features.auth.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.permission.AccessEntry;
import com.akandiah.propmanager.common.permission.Actions;
import com.akandiah.propmanager.common.permission.PermissionDomains;
import com.akandiah.propmanager.common.permission.PermissionMaskUtil;
import com.akandiah.propmanager.features.lease.domain.LeaseTenant;
import com.akandiah.propmanager.features.lease.domain.LeaseTenantRepository;
import com.akandiah.propmanager.features.organization.domain.MemberScope;
import com.akandiah.propmanager.features.organization.domain.MemberScopeRepository;
import com.akandiah.propmanager.features.organization.domain.Membership;
import com.akandiah.propmanager.features.organization.domain.MembershipRepository;
import com.akandiah.propmanager.features.organization.domain.ScopeType;
import com.akandiah.propmanager.features.prop.domain.Prop;
import com.akandiah.propmanager.features.prop.domain.PropRepository;
import com.akandiah.propmanager.features.unit.domain.Unit;

import lombok.RequiredArgsConstructor;

/**
 * Builds the "access" list from three sources:
 * <ol>
 *   <li>Explicit member scopes (MemberScope rows)</li>
 *   <li>Property ownership (Prop.ownerId → full CRUD on all domains)</li>
 *   <li>Active lease tenancy (LeaseTenant → READ on leases domain)</li>
 * </ol>
 * Entries with the same (orgId, scopeType, scopeId) are merged by ORing bitmasks.
 * Total query count: exactly 4, regardless of org/scope/property count.
 */
@Service
@RequiredArgsConstructor
public class JwtHydrationService {

	private final MembershipRepository membershipRepository;
	private final MemberScopeRepository memberScopeRepository;
	private final PropRepository propRepository;
	private final LeaseTenantRepository leaseTenantRepository;

	private static final int FULL_CRUD = Actions.READ | Actions.CREATE | Actions.UPDATE | Actions.DELETE;

	/** Owner: full CRUD on all 4 domains for each owned property. */
	private static final Map<String, Integer> OWNER_MASKS = Map.of(
			PermissionDomains.LEASES, FULL_CRUD,
			PermissionDomains.MAINTENANCE, FULL_CRUD,
			PermissionDomains.FINANCES, FULL_CRUD,
			PermissionDomains.TENANTS, FULL_CRUD);

	/** Tenant: read-only on leases domain only. */
	private static final Map<String, Integer> TENANT_MASKS = Map.of(
			PermissionDomains.LEASES, Actions.READ);

	/**
	 * Loads all access entries for the user from memberships, property ownership,
	 * and active lease tenancy. Deduplicates by merging bitmasks for identical scopes.
	 *
	 * @param userId the user's ID
	 * @return deduplicated list of access entries (empty if user has no access)
	 */
	@Transactional(readOnly = true)
	public List<AccessEntry> hydrate(UUID userId) {
		List<AccessEntry> rawAccess = new ArrayList<>();

		// 1. Explicit member scopes
		hydrateMemberScopes(userId, rawAccess);

		// 2. Property ownership — full CRUD on all domains
		hydratePropertyOwnership(userId, rawAccess);

		// 3. Active lease tenancy — READ on leases domain
		hydrateActiveTenancies(userId, rawAccess);

		// 4. Deduplicate: merge entries with same (orgId, scopeType, scopeId)
		return deduplicateAccess(rawAccess);
	}

	private void hydrateMemberScopes(UUID userId, List<AccessEntry> access) {
		List<Membership> memberships = membershipRepository.findByUserIdWithUserAndOrg(userId);
		if (memberships.isEmpty()) {
			return;
		}

		List<UUID> membershipIds = memberships.stream().map(Membership::getId).toList();
		Map<UUID, List<MemberScope>> scopesByMembership = memberScopeRepository
				.findByMembershipIdIn(membershipIds)
				.stream()
				.collect(Collectors.groupingBy(s -> s.getMembership().getId()));

		for (Membership m : memberships) {
			UUID orgId = m.getOrganization().getId();
			List<MemberScope> scopes = scopesByMembership.getOrDefault(m.getId(), List.of());
			for (MemberScope scope : scopes) {
				Map<String, Integer> masks = permissionsToMasks(scope.getPermissions());
				UUID scopeId = scope.getScopeType() == ScopeType.ORG
						? orgId
						: scope.getScopeId();
				access.add(new AccessEntry(orgId, scope.getScopeType().toResourceType().name(), scopeId, masks));
			}
		}
	}

	private void hydratePropertyOwnership(UUID userId, List<AccessEntry> access) {
		List<Prop> ownedProps = propRepository.findByOwnerIdWithOrganization(userId);
		for (Prop prop : ownedProps) {
			access.add(new AccessEntry(
					prop.getOrganization().getId(), "PROPERTY", prop.getId(), OWNER_MASKS));
		}
	}

	private void hydrateActiveTenancies(UUID userId, List<AccessEntry> access) {
		List<LeaseTenant> tenancies = leaseTenantRepository.findActiveByUserIdWithLeaseUnitPropOrg(userId);
		for (LeaseTenant lt : tenancies) {
			Unit unit = lt.getLease().getUnit();
			UUID orgId = unit.getProp().getOrganization().getId();
			access.add(new AccessEntry(orgId, "UNIT", unit.getId(), TENANT_MASKS));
		}
	}

	static List<AccessEntry> deduplicateAccess(List<AccessEntry> rawAccess) {
		if (rawAccess.size() <= 1) {
			return List.copyOf(rawAccess);
		}
		Map<String, AccessEntry> merged = new LinkedHashMap<>();
		for (AccessEntry e : rawAccess) {
			String key = e.orgId() + ":" + e.scopeType() + ":" + e.scopeId();
			merged.merge(key, e, JwtHydrationService::mergeEntries);
		}
		return List.copyOf(merged.values());
	}

	private static AccessEntry mergeEntries(AccessEntry a, AccessEntry b) {
		Map<String, Integer> mergedMasks = new LinkedHashMap<>(a.permissions());
		b.permissions().forEach((domain, mask) ->
				mergedMasks.merge(domain, mask, (m1, m2) -> m1 | m2));
		return new AccessEntry(a.orgId(), a.scopeType(), a.scopeId(), mergedMasks);
	}

	private static Map<String, Integer> permissionsToMasks(Map<String, String> permissions) {
		if (permissions == null || permissions.isEmpty()) {
			return Map.of();
		}
		Map<String, Integer> out = new LinkedHashMap<>();
		for (String domain : PermissionDomains.VALID_KEYS) {
			String letters = permissions.get(domain);
			if (letters != null && !letters.isEmpty()) {
				out.put(domain, PermissionMaskUtil.parseToMask(letters));
			}
		}
		return Map.copyOf(out);
	}
}
