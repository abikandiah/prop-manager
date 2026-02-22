package com.akandiah.propmanager.features.auth.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.permission.AccessEntry;
import com.akandiah.propmanager.common.permission.PermissionDomains;
import com.akandiah.propmanager.common.permission.PermissionMaskUtil;
import com.akandiah.propmanager.features.organization.domain.MemberScope;
import com.akandiah.propmanager.features.organization.domain.MemberScopeRepository;
import com.akandiah.propmanager.features.organization.domain.Membership;
import com.akandiah.propmanager.features.organization.domain.MembershipRepository;

import lombok.RequiredArgsConstructor;

/**
 * Builds the "access" list for JWT: loads membership + scopes for a user,
 * computes effective permissions per scope (bitmasks), returns a list of
 * {@link AccessEntry} for inclusion in the token or request attribute.
 */
@Service
@RequiredArgsConstructor
public class JwtHydrationService {

	private final MembershipRepository membershipRepository;
	private final MemberScopeRepository memberScopeRepository;

	/**
	 * Loads all memberships and member scopes for the user, computes effective
	 * permissions (domain → bitmask) per scope, and returns the list for JWT claim.
	 * Org-level: membership permissions. Scope-level: OR of membership and scope
	 * permissions per domain so scope can only add, not remove.
	 *
	 * @param userId the user's ID
	 * @return list of access entries (empty if user has no memberships)
	 */
	@Transactional(readOnly = true)
	public List<AccessEntry> hydrate(UUID userId) {
		List<Membership> memberships = membershipRepository.findByUserIdWithUserAndOrg(userId);
		List<AccessEntry> access = new ArrayList<>();

		for (Membership m : memberships) {
			UUID orgId = m.getOrganization().getId();
			Map<String, Integer> orgMasks = permissionsToMasks(m.getPermissions());

			// Org-level: one entry per membership
			access.add(new AccessEntry(orgId, "ORG", orgId, orgMasks));

			List<MemberScope> scopes = memberScopeRepository.findByMembershipId(m.getId());
			for (MemberScope scope : scopes) {
				Map<String, Integer> scopeMasks = permissionsToMasks(scope.getPermissions());
				Map<String, Integer> effective = mergeMasks(orgMasks, scopeMasks);
				access.add(new AccessEntry(orgId, scope.getScopeType().name(), scope.getScopeId(), effective));
			}
		}

		return List.copyOf(access);
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

	/** OR masks per domain: effective has all bits from base and scope. */
	private static Map<String, Integer> mergeMasks(Map<String, Integer> base, Map<String, Integer> scope) {
		Map<String, Integer> out = new LinkedHashMap<>(base);
		for (Map.Entry<String, Integer> e : scope.entrySet()) {
			out.merge(e.getKey(), e.getValue(), (a, b) -> a | b);
		}
		return Map.copyOf(out);
	}
}
