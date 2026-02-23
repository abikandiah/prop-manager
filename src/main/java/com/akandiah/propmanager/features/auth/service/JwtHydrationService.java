package com.akandiah.propmanager.features.auth.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.permission.AccessEntry;
import com.akandiah.propmanager.common.permission.PermissionDomains;
import com.akandiah.propmanager.common.permission.PermissionMaskUtil;
import com.akandiah.propmanager.features.organization.domain.MemberScope;
import com.akandiah.propmanager.features.organization.domain.MemberScopeRepository;
import com.akandiah.propmanager.features.organization.domain.Membership;
import com.akandiah.propmanager.features.organization.domain.MembershipRepository;
import com.akandiah.propmanager.features.organization.domain.ScopeType;

import lombok.RequiredArgsConstructor;

/**
 * Builds the "access" list from member scopes only.
 * Each MemberScope row becomes one AccessEntry.
 * Scopes are batch-loaded to avoid N+1 queries.
 *
 * <p>Automatic permissions (prop owner, active lease tenant) are added in Piece 14.
 */
@Service
@RequiredArgsConstructor
public class JwtHydrationService {

	private final MembershipRepository membershipRepository;
	private final MemberScopeRepository memberScopeRepository;

	/**
	 * Loads all memberships and member scopes for the user, computes bitmasks per scope,
	 * and returns the access list. One AccessEntry per MemberScope row.
	 *
	 * @param userId the user's ID
	 * @return list of access entries (empty if user has no memberships or scopes)
	 */
	@Transactional(readOnly = true)
	public List<AccessEntry> hydrate(UUID userId) {
		List<Membership> memberships = membershipRepository.findByUserIdWithUserAndOrg(userId);
		if (memberships.isEmpty()) {
			return List.of();
		}

		List<UUID> membershipIds = memberships.stream().map(Membership::getId).toList();
		Map<UUID, List<MemberScope>> scopesByMembership = memberScopeRepository
				.findByMembershipIdIn(membershipIds)
				.stream()
				.collect(Collectors.groupingBy(s -> s.getMembership().getId()));

		List<AccessEntry> access = new ArrayList<>();
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

		// Automatic permissions (prop owner + active lease tenant) are added in Piece 14
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
}
