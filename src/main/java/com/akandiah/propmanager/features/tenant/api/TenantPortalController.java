package com.akandiah.propmanager.features.tenant.api;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.akandiah.propmanager.features.tenant.api.dto.TenantLeaseView;
import com.akandiah.propmanager.features.tenant.service.TenantPortalService;
import com.akandiah.propmanager.features.user.domain.User;
import com.akandiah.propmanager.security.JwtUserResolver;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Tenant-facing portal endpoints (/api/tenant/...).
 * Separate from the manager-facing TenantController (/api/tenants/...).
 */
@RestController
@RequestMapping("/api/tenant")
@RequiredArgsConstructor
@Tag(name = "Tenant portal", description = "Tenant self-service portal endpoints")
public class TenantPortalController {

	private final TenantPortalService tenantPortalService;
	private final JwtUserResolver jwtUserResolver;

	@GetMapping("/my-lease")
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "Get current tenant's lease view",
			description = "Returns a read-only aggregate of the tenant's active lease in the given org. "
					+ "Returns 403 if not a member of the org, 404 if no active lease in this org.")
	public ResponseEntity<TenantLeaseView> getMyLease(
			@RequestParam UUID orgId,
			@AuthenticationPrincipal Jwt jwt) {
		User user = jwtUserResolver.resolve(jwt);
		return ResponseEntity.ok(tenantPortalService.getMyLease(user.getId(), orgId));
	}
}
