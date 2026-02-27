package com.akandiah.propmanager.features.auth.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
import com.akandiah.propmanager.features.membership.domain.MemberScope;
import com.akandiah.propmanager.features.membership.domain.MemberScopeRepository;
import com.akandiah.propmanager.features.membership.domain.Membership;
import com.akandiah.propmanager.features.membership.domain.MembershipRepository;
import com.akandiah.propmanager.features.membership.domain.MembershipTemplate;
import com.akandiah.propmanager.features.membership.domain.MembershipTemplateItem;
import com.akandiah.propmanager.features.prop.domain.Prop;
import com.akandiah.propmanager.features.prop.domain.PropRepository;
import com.akandiah.propmanager.features.unit.domain.Unit;

import lombok.RequiredArgsConstructor;

/**
 * Builds the "access" list from three sources:
 * <ol>
 * <li>Membership template items (live resolution) + MemberScope rows (additive
 * custom permissions)</li>
 * <li>Property ownership (Prop.ownerId → full CRUD on all domains at PROPERTY
 * scope)</li>
 * <li>Active lease tenancy (LeaseTenant → READ on leases domain at UNIT
 * scope)</li>
 * </ol>
 *
 * <h3>Template resolution algorithm</h3>
 * For each membership that has a {@code membershipTemplate}:
 * <ul>
 * <li>ORG items → always emit an AccessEntry at (ORG, orgId)</li>
 * <li>PROPERTY items → emit one AccessEntry per MemberScope binding row of
 * scopeType=PROPERTY</li>
 * <li>UNIT items → emit one AccessEntry per MemberScope binding row of
 * scopeType=UNIT</li>
 * </ul>
 * All MemberScope rows with non-empty permissions are also emitted (additive).
 * Entries with the same (orgId, scopeType, scopeId) are merged by ORing
 * bitmasks.
 */
@Service
@RequiredArgsConstructor
public class JwtHydrationService {

	private final MembershipRepository membershipRepository;
	private final MemberScopeRepository memberScopeRepository;
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
		// Fetches memberships with user, org, and template (single query via JOIN
		// FETCH)
		List<Membership> memberships = membershipRepository.findByUserIdWithUserOrgAndTemplate(userId);
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

			// --- Template-based permissions ---
			MembershipTemplate template = m.getMembershipTemplate();
			if (template != null) {
				for (MembershipTemplateItem item : template.getItems()) {
					Map<String, Integer> masks = permissionsToMasks(item.getPermissions());
					if (masks.isEmpty()) {
						continue;
					}
					switch (item.getScopeType()) {
						case ORG ->
							// ORG items always activate — no binding row needed
							access.add(new AccessEntry(orgId, ResourceType.ORG, orgId, masks));
						case PROPERTY ->
							// PROPERTY items activate per binding row
							scopes.stream()
									.filter(s -> s.getScopeType() == ResourceType.PROPERTY)
									.forEach(s -> access.add(
											new AccessEntry(orgId, ResourceType.PROPERTY, s.getScopeId(), masks)));
						case UNIT ->
							// UNIT items activate per binding row
							scopes.stream()
									.filter(s -> s.getScopeType() == ResourceType.UNIT)
									.forEach(s -> access.add(
											new AccessEntry(orgId, ResourceType.UNIT, s.getScopeId(), masks)));
						case ASSET ->
							// ASSET items activate per binding row
							scopes.stream()
									.filter(s -> s.getScopeType() == ResourceType.ASSET)
									.forEach(s -> access.add(
											new AccessEntry(orgId, ResourceType.ASSET, s.getScopeId(), masks)));
					}
				}
			}

			// --- Explicit / additive scope permissions ---
			for (MemberScope scope : scopes) {
				if (scope.getPermissions() == null || scope.getPermissions().isEmpty()) {
					continue; // pure binding row — no additive contribution
				}
				Map<String, Integer> masks = permissionsToMasks(scope.getPermissions());
				if (masks.isEmpty()) {
					continue;
				}
				UUID scopeId = scope.getScopeType() == ResourceType.ORG ? orgId : scope.getScopeId();
				access.add(new AccessEntry(orgId, scope.getScopeType(), scopeId, masks));
			}
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
