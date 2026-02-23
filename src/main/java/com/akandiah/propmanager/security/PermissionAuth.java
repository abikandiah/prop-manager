package com.akandiah.propmanager.security;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.akandiah.propmanager.common.permission.AccessEntry;
import com.akandiah.propmanager.common.permission.ResourceType;
import com.akandiah.propmanager.features.lease.domain.Lease;
import com.akandiah.propmanager.features.lease.domain.LeaseRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * Bean for use in @PreAuthorize SpEL: checks hierarchy-aware permissions using
 * the hydrated access list from the request attribute.
 * <p>
 * Example: {@code @PreAuthorize("@permissionAuth.hasAccess(T(com.akandiah.propmanager.common.permission.Actions).READ, 'l', T(com.akandiah.propmanager.common.permission.ResourceType).UNIT, #unitId, #orgId)")}
 */
@Component("permissionAuth")
@RequiredArgsConstructor
public class PermissionAuth {

	private final HierarchyAwareAuthorizationService authorizationService;
	private final LeaseRepository leaseRepository;

	/**
	 * Returns true if the current user's access list grants the required action
	 * for the given resource in the given domain. Reads the access list from the
	 * request attribute set by {@link JwtAccessHydrationFilter}.
	 *
	 * @param requiredAction action bit, e.g. {@link com.akandiah.propmanager.common.permission.Actions#READ}
	 * @param domain         domain key (l, m, f, t)
	 * @param resourceType   ORG, PROPERTY, or UNIT
	 * @param resourceId     id of the resource
	 * @param orgId          organization context
	 * @return true if access is granted
	 */
	@SuppressWarnings("unused")
	public boolean hasAccess(int requiredAction, String domain, ResourceType resourceType,
			UUID resourceId, UUID orgId) {
		List<AccessEntry> access = getAccessFromRequest();
		return authorizationService.allow(access, requiredAction, domain, resourceType, resourceId, orgId);
	}

	/**
	 * Convenience method for lease endpoints: looks up the lease by ID,
	 * derives the unit ID, then checks UNIT-level access.
	 * Returns false if the lease does not exist.
	 *
	 * @param requiredAction action bit
	 * @param domain         domain key (l, m, f, t)
	 * @param leaseId        lease ID (resolved to its unit for the UNIT-scope check)
	 * @param orgId          organization context
	 * @return true if access is granted
	 */
	@SuppressWarnings("unused")
	public boolean hasLeaseAccess(int requiredAction, String domain, UUID leaseId, UUID orgId) {
		return leaseRepository.findById(leaseId)
				.map(Lease::getUnit)
				.map(unit -> {
					List<AccessEntry> access = getAccessFromRequest();
					return authorizationService.allow(
							access, requiredAction, domain, ResourceType.UNIT, unit.getId(), orgId);
				})
				.orElse(false);
	}

	@SuppressWarnings("unchecked")
	private List<AccessEntry> getAccessFromRequest() {
		ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		if (attrs == null) {
			return List.of();
		}
		HttpServletRequest request = attrs.getRequest();
		Object attr = request.getAttribute(JwtAccessHydrationFilter.REQUEST_ATTRIBUTE_ACCESS);
		return attr instanceof List<?> list ? (List<AccessEntry>) list : List.of();
	}
}
