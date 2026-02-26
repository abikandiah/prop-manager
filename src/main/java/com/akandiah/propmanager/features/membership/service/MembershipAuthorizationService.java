package com.akandiah.propmanager.features.membership.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.permission.AccessEntry;
import com.akandiah.propmanager.common.permission.Actions;
import com.akandiah.propmanager.common.permission.PermissionDomains;
import com.akandiah.propmanager.common.permission.ResourceType;
import com.akandiah.propmanager.features.membership.domain.Membership;
import com.akandiah.propmanager.features.membership.domain.MembershipRepository;
import com.akandiah.propmanager.common.util.SecurityUtils;
import com.akandiah.propmanager.security.HierarchyAwareAuthorizationService;
import com.akandiah.propmanager.security.JwtUserResolver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * SpEL bean for {@code @PreAuthorize} on {@code MembershipController} and
 * {@code MemberScopeController}. Handles membership-specific authorization:
 * owner checks and org-admin checks via the permission scope hierarchy.
 */
@Service("membershipAuth")
@Slf4j
@RequiredArgsConstructor
public class MembershipAuthorizationService {

	private final MembershipRepository membershipRepository;
	private final HierarchyAwareAuthorizationService authorizationService;
	private final JwtUserResolver jwtUserResolver;

	/**
	 * Allows access if the caller is:
	 * <ul>
	 *   <li>System Admin, OR</li>
	 *   <li>The user who owns the membership (membership.user == currentUser), OR</li>
	 *   <li>An Org Admin — has READ on the {@code 'o'} domain at ORG scope.</li>
	 * </ul>
	 * Used for: {@code GET /memberships/{id}}.
	 */
	@Transactional(readOnly = true)
	public boolean canView(UUID membershipId) {
		try {
			if (SecurityUtils.isGlobalAdmin()) {
				return true;
			}
			Membership m = membershipRepository.findByIdWithOrganizationAndUser(membershipId).orElse(null);
			if (m == null) {
				return false;
			}
			return isOwnerOrOrgAdmin(m, Actions.READ);
		} catch (Exception e) {
			log.error("Error checking canView for membership {}", membershipId, e);
			return false;
		}
	}

	/**
	 * Same as {@link #canView(UUID)} but also verifies the membership belongs to the given org.
	 * Returns {@code false} (403) if the membership/org combination is invalid, preventing
	 * IDOR via URL path manipulation.
	 *
	 * <p>Used for: {@code GET .../scopes}, {@code GET .../scopes/{scopeId}}.
	 */
	@Transactional(readOnly = true)
	public boolean canView(UUID membershipId, UUID orgId) {
		try {
			if (SecurityUtils.isGlobalAdmin()) {
				return true;
			}
			Membership m = membershipRepository.findByIdWithOrganizationAndUser(membershipId).orElse(null);
			if (m == null || !m.getOrganization().getId().equals(orgId)) {
				return false;
			}
			return isOwnerOrOrgAdmin(m, Actions.READ);
		} catch (Exception e) {
			log.error("Error checking canView for membership {} in org {}", membershipId, orgId, e);
			return false;
		}
	}

	/**
	 * Allows access for scope write operations (POST/PATCH/DELETE on scopes) if the caller is:
	 * <ul>
	 *   <li>System Admin, OR</li>
	 *   <li>An Org Admin — has the required action on the {@code 'o'} domain at ORG scope.</li>
	 * </ul>
	 * Also verifies the membership belongs to the given org to prevent IDOR via URL manipulation.
	 *
	 * @param action bitmask from {@link Actions} (CREATE=2, UPDATE=4, DELETE=8)
	 */
	@Transactional(readOnly = true)
	public boolean canManageScopes(int action, UUID orgId, UUID membershipId) {
		try {
			if (SecurityUtils.isGlobalAdmin()) {
				return true;
			}
			// Org path consistency — membership must belong to this org
			if (!membershipRepository.findByIdAndOrganizationId(membershipId, orgId).isPresent()) {
				return false;
			}
			List<AccessEntry> access = SecurityUtils.getAccessFromRequest();
			return authorizationService.allow(access, action, PermissionDomains.ORGANIZATION,
					ResourceType.ORG, orgId, orgId);
		} catch (Exception e) {
			log.error("Error checking canManageScopes (action={}) for membership {} in org {}",
					action, membershipId, orgId, e);
			return false;
		}
	}

	/**
	 * Allows access for template-assignment write operations (apply-template) if the caller is:
	 * <ul>
	 *   <li>System Admin, OR</li>
	 *   <li>An Org Admin — has UPDATE on the {@code 'o'} domain at ORG scope.</li>
	 * </ul>
	 * Used for: {@code POST /memberships/{id}/apply-template}.
	 */
	@Transactional(readOnly = true)
	public boolean canManage(UUID membershipId) {
		try {
			if (SecurityUtils.isGlobalAdmin()) {
				return true;
			}
			Membership m = membershipRepository.findByIdWithOrganizationAndUser(membershipId).orElse(null);
			if (m == null) {
				return false;
			}
			UUID orgId = m.getOrganization().getId();
			List<AccessEntry> access = SecurityUtils.getAccessFromRequest();
			return authorizationService.allow(access, Actions.UPDATE, PermissionDomains.ORGANIZATION,
					ResourceType.ORG, orgId, orgId);
		} catch (Exception e) {
			log.error("Error checking canManage for membership {}", membershipId, e);
			return false;
		}
	}

	/**
	 * Allows access if the caller is:
	 * <ul>
	 *   <li>System Admin, OR</li>
	 *   <li>The user who owns the membership (self-removal).</li>
	 * </ul>
	 * Used for: {@code GET /memberships?userId=X}.
	 */
	public boolean canListFor(UUID userId) {
		if (SecurityUtils.isGlobalAdmin()) {
			return true;
		}
		return jwtUserResolver.resolveOptionalId()
				.map(id -> id.equals(userId))
				.orElse(false);
	}

	private boolean isOwnerOrOrgAdmin(Membership m, int action) {
		UUID currentUserId = jwtUserResolver.resolveOptionalId().orElse(null);
		if (currentUserId == null) {
			return false;
		}
		if (m.getUser() != null && m.getUser().getId().equals(currentUserId)) {
			return true;
		}
		UUID orgId = m.getOrganization().getId();
		List<AccessEntry> access = SecurityUtils.getAccessFromRequest();
		return authorizationService.allow(access, action, PermissionDomains.ORGANIZATION,
				ResourceType.ORG, orgId, orgId);
	}

}
