package com.akandiah.propmanager.features.auth.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.permission.AccessEntry;
import com.akandiah.propmanager.common.permission.Actions;
import com.akandiah.propmanager.common.permission.PermissionDomains;
import com.akandiah.propmanager.common.permission.PermissionMaskUtil;
import com.akandiah.propmanager.common.permission.ResourceType;
import com.akandiah.propmanager.config.CacheConfig;
import com.akandiah.propmanager.features.lease.domain.LeaseTenant;
import com.akandiah.propmanager.features.lease.domain.LeaseTenantRepository;
import com.akandiah.propmanager.features.membership.domain.PolicyAssignment;
import com.akandiah.propmanager.features.membership.domain.PolicyAssignmentRepository;
import com.akandiah.propmanager.features.membership.domain.Membership;
import com.akandiah.propmanager.features.membership.domain.MembershipRepository;
import com.akandiah.propmanager.features.prop.domain.Prop;
import com.akandiah.propmanager.features.prop.domain.PropRepository;
import com.akandiah.propmanager.features.unit.domain.Unit;

import lombok.RequiredArgsConstructor;

/**
 * Builds the "access" list from three sources:
 * <ol>
 * <li>PolicyAssignment rows — each assignment resolves effective permissions from
 * its overrides (if present) or its linked PermissionPolicy</li>
 * <li>Property ownership (Prop.ownerId → full CRUD on all domains at PROPERTY scope)</li>
 * <li>Active lease tenancy (LeaseTenant → READ on leases domain at UNIT scope)</li>
 * </ol>
 *
 * Entries with the same (orgId, resourceType, resourceId) are merged by ORing bitmasks.
 */
@Service
@RequiredArgsConstructor
public class JwtHydrationService {

	private final MembershipRepository membershipRepository;
	private final PolicyAssignmentRepository policyAssignmentRepository;
	private final PropRepository propRepository;
	private final LeaseTenantRepository leaseTenantRepository;

	private static final int FULL_CRUD = Actions.READ | Actions.CREATE | Actions.UPDATE | Actions.DELETE;

	/**
	 * Owner: full CRUD on all operational and structural domains for each owned
	 * property.
	 */
	private static final Map<String, Integer> OWNER_MASKS = Map.of(
			PermissionDomains.PORTFOLIO, FULL_CRUD,
			PermissionDomains.LEASES, FULL_CRUD,
			PermissionDomains.MAINTENANCE, FULL_CRUD,
			PermissionDomains.FINANCES, FULL_CRUD,
			PermissionDomains.TENANTS, FULL_CRUD);

	/** Tenant: read-only on leases and maintenance domains. */
	private static final Map<String, Integer> TENANT_MASKS = Map.of(
			PermissionDomains.LEASES, Actions.READ,
			PermissionDomains.MAINTENANCE, Actions.READ);

	@CacheEvict(value = CacheConfig.CACHE_PERMISSIONS, key = "#userId")
	public void evict(UUID userId) {
		// Intentionally empty — @CacheEvict removes the cached entry for this userId.
	}

	@Cacheable(value = CacheConfig.CACHE_PERMISSIONS, key = "#userId", sync = true)
	@Transactional(readOnly = true)
	public List<AccessEntry> hydrate(UUID userId) {
		List<AccessEntry> rawAccess = new ArrayList<>();

		// 1. Memberships: template-based + custom scope permissions
		hydrateMemberships(userId, rawAccess);

		// 2. Property ownership — full CRUD on all domains at PROPERTY scope
		hydratePropertyOwnership(userId, rawAccess);

		// 3. Active lease tenancy — READ on leases at UNIT scope
		hydrateActiveTenancies(userId, rawAccess);

		// 4. Deduplicate: merge entries with same (orgId, scopeType, scopeId) by ORing
		// bitmasks
		return deduplicateAccess(rawAccess);
	}

	private void hydrateMemberships(UUID userId, List<AccessEntry> access) {
		List<Membership> memberships = membershipRepository.findByUserIdWithUserAndOrgForHydration(userId);
		if (memberships.isEmpty()) {
			return;
		}

		List<UUID> membershipIds = memberships.stream().map(Membership::getId).toList();
		Map<UUID, UUID> orgByMembership = memberships.stream()
				.collect(Collectors.toMap(Membership::getId, m -> m.getOrganization().getId()));

		// Eager-load assignments with their policy in one query
		List<PolicyAssignment> allAssignments = policyAssignmentRepository
				.findByMembershipIdInWithPolicy(membershipIds);

		for (PolicyAssignment assignment : allAssignments) {
			UUID orgId = orgByMembership.get(assignment.getMembership().getId());
			if (orgId == null) {
				continue;
			}

			Map<String, String> effectivePermissions;
			if (assignment.getOverrides() != null && !assignment.getOverrides().isEmpty()) {
				effectivePermissions = assignment.getOverrides();
			} else if (assignment.getPolicy() != null
					&& assignment.getPolicy().getPermissions() != null
					&& !assignment.getPolicy().getPermissions().isEmpty()) {
				effectivePermissions = assignment.getPolicy().getPermissions();
			} else {
				continue; // skip empty assignments
			}

			Map<String, Integer> masks = permissionsToMasks(effectivePermissions);
			if (masks.isEmpty()) {
				continue;
			}

			UUID resourceId = assignment.getResourceType() == ResourceType.ORG
					? orgId
					: assignment.getResourceId();
			access.add(new AccessEntry(orgId, assignment.getResourceType(), resourceId, masks));
		}
	}

	private void hydratePropertyOwnership(UUID userId, List<AccessEntry> access) {
		List<Prop> ownedProps = propRepository.findByOwnerIdWithOrganization(userId);
		for (Prop prop : ownedProps) {
			access.add(new AccessEntry(
					prop.getOrganization().getId(), ResourceType.PROPERTY, prop.getId(), OWNER_MASKS));
		}
	}

	private void hydrateActiveTenancies(UUID userId, List<AccessEntry> access) {
		List<LeaseTenant> tenancies = leaseTenantRepository.findActiveByUserIdWithLeaseUnitPropOrg(userId);
		for (LeaseTenant lt : tenancies) {
			Unit unit = lt.getLease().getUnit();
			UUID orgId = unit.getProp().getOrganization().getId();
			access.add(new AccessEntry(orgId, ResourceType.UNIT, unit.getId(), TENANT_MASKS));
		}
	}

	static List<AccessEntry> deduplicateAccess(List<AccessEntry> rawAccess) {
		if (rawAccess.size() <= 1) {
			return List.copyOf(rawAccess);
		}
		Map<String, AccessEntry> merged = new LinkedHashMap<>();
		for (AccessEntry e : rawAccess) {
			String key = e.orgId() + ":" + e.scopeType().name() + ":" + e.scopeId();
			merged.merge(key, e, JwtHydrationService::mergeEntries);
		}
		return List.copyOf(merged.values());
	}

	private static AccessEntry mergeEntries(AccessEntry a, AccessEntry b) {
		Map<String, Integer> mergedMasks = new LinkedHashMap<>(a.permissions());
		b.permissions().forEach((domain, mask) -> mergedMasks.merge(domain, mask, (m1, m2) -> m1 | m2));
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
