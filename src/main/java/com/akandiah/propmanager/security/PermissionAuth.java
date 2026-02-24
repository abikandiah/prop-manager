package com.akandiah.propmanager.security;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.akandiah.propmanager.common.permission.AccessEntry;
import com.akandiah.propmanager.common.permission.ResourceType;
import com.akandiah.propmanager.features.lease.domain.LeaseRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/** SpEL bean for @PreAuthorize: checks hierarchy-aware permissions from the hydrated access list. */
@Component("permissionAuth")
@RequiredArgsConstructor
public class PermissionAuth {

	private final HierarchyAwareAuthorizationService authorizationService;
	private final LeaseRepository leaseRepository;

	public boolean hasAccess(int requiredAction, String domain, ResourceType resourceType,
			UUID resourceId, UUID orgId) {
		List<AccessEntry> access = getAccessFromRequest();
		return authorizationService.allow(access, requiredAction, domain, resourceType, resourceId, orgId);
	}

	/** Resolves the lease's unit, then checks UNIT-level access. Returns false if lease not found. */
	public boolean hasLeaseAccess(int requiredAction, String domain, UUID leaseId, UUID orgId) {
		return leaseRepository.findUnitIdById(leaseId)
				.map(unitId -> {
					List<AccessEntry> access = getAccessFromRequest();
					return authorizationService.allow(
							access, requiredAction, domain, ResourceType.UNIT, unitId, orgId);
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
