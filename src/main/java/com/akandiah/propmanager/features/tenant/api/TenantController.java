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
import org.springframework.web.bind.annotation.RestController;

import com.akandiah.propmanager.features.tenant.api.dto.TenantResponse;
import com.akandiah.propmanager.features.tenant.api.dto.UpdateTenantRequest;
import com.akandiah.propmanager.features.tenant.service.TenantService;
import com.akandiah.propmanager.features.user.domain.User;
import com.akandiah.propmanager.features.user.service.UserService;

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
	private final UserService userService;

	// ───────────────────────── Admin queries ─────────────────────────

	@GetMapping
	@Operation(summary = "List all tenants")
	public ResponseEntity<List<TenantResponse>> list() {
		return ResponseEntity.ok(tenantService.findAll());
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get tenant by ID")
	public ResponseEntity<TenantResponse> getById(@PathVariable UUID id) {
		return ResponseEntity.ok(tenantService.findById(id));
	}

	// ───────────────────────── Current user profile ─────────────────────────

	@GetMapping("/me")
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "Get the current user's tenant profile",
			description = "Returns 404 if the user has not yet accepted a lease invite.")
	public ResponseEntity<TenantResponse> getMe(@AuthenticationPrincipal Jwt jwt) {
		User user = getCurrentUser(jwt);
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
		User user = getCurrentUser(jwt);
		return ResponseEntity.ok(tenantService.updateByUser(user, req));
	}

	// ───────────────────────── Helpers ─────────────────────────

	private User getCurrentUser(Jwt jwt) {
		return userService.findUserFromJwt(jwt)
				.orElseThrow(() -> new IllegalStateException("User not found for authenticated subject"));
	}
}
