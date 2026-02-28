package com.akandiah.propmanager.features.tenant.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.akandiah.propmanager.common.permission.AccessListUtil;
import com.akandiah.propmanager.common.permission.Actions;
import com.akandiah.propmanager.common.permission.PermissionDomains;
import com.akandiah.propmanager.common.permission.AccessListUtil.ScopedAccessFilter;
import com.akandiah.propmanager.common.util.SecurityUtils;
import com.akandiah.propmanager.features.tenant.api.dto.TenantResponse;
import com.akandiah.propmanager.features.tenant.api.dto.UpdateTenantRequest;
import com.akandiah.propmanager.features.tenant.service.TenantService;
import com.akandiah.propmanager.features.user.domain.User;
import com.akandiah.propmanager.security.JwtUserResolver;

import jakarta.servlet.http.HttpServletRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenants", description = "Tenant profile management")
public class TenantController {

	private final TenantService tenantService;
	private final JwtUserResolver jwtUserResolver;

	// ───────────────────────── Scoped queries ─────────────────────────

	@GetMapping
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "List tenants",
			description = "Admins see all tenants. Authenticated users with LEASES:READ access see only tenants on their managed properties/units. Pass activeOnly=false to include tenants from expired leases.")
	public ResponseEntity<List<TenantResponse>> list(
			@RequestParam(defaultValue = "true") boolean activeOnly,
			HttpServletRequest request) {
		if (SecurityUtils.isGlobalAdmin()) {
			return ResponseEntity.ok(tenantService.findAll());
		}
		ScopedAccessFilter filter = AccessListUtil.forScopedResources(
				AccessListUtil.fromRequest(request), PermissionDomains.TENANTS, Actions.READ);
		return ResponseEntity.ok(tenantService.findAll(filter, activeOnly));
	}

	@GetMapping("/{id}")
	@PreAuthorize("@permissionGuard.hasTenantAccess('READ', 'TENANTS', #id, #orgId)")
	@Operation(summary = "Get tenant by ID",
			description = "Accessible to property managers and owners with LEASES:READ access on the tenant's unit, property, or organization. Tenant users cannot view other tenants' profiles.")
	public ResponseEntity<TenantResponse> getById(
			@PathVariable UUID id,
			@RequestParam UUID orgId) {
		return ResponseEntity.ok(tenantService.findById(id));
	}

	// ───────────────────────── Current user profile ─────────────────────────

	@GetMapping("/me")
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "Get the current user's tenant profile",
			description = "Returns 404 if the user has not yet accepted a lease invite.")
	public ResponseEntity<TenantResponse> getMe(@AuthenticationPrincipal Jwt jwt) {
		User user = jwtUserResolver.resolve(jwt);
		return tenantService.findByUser(user)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	@PatchMapping("/me")
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "Update the current user's tenant profile",
			description = "Updates rental-context fields. Returns 404 if no profile exists yet.")
	public ResponseEntity<TenantResponse> updateMe(
			@Valid @RequestBody UpdateTenantRequest req,
			@AuthenticationPrincipal Jwt jwt) {
		User user = jwtUserResolver.resolve(jwt);
		return ResponseEntity.ok(tenantService.updateByUser(user, req));
	}
}
