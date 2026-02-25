package com.akandiah.propmanager.features.lease.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.akandiah.propmanager.features.lease.api.dto.InviteLeaseTenantRequest;
import com.akandiah.propmanager.features.lease.api.dto.LeaseTenantResponse;
import com.akandiah.propmanager.features.lease.service.LeaseTenantService;
import com.akandiah.propmanager.features.user.domain.User;
import com.akandiah.propmanager.security.JwtUserResolver;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/leases/{leaseId}/tenants")
@RequiredArgsConstructor
@Tag(name = "Lease Tenants", description = "Manage tenants for a lease agreement")
public class LeaseTenantController {

	private final LeaseTenantService leaseTenantService;
	private final JwtUserResolver jwtUserResolver;

	@GetMapping
	@Operation(summary = "List tenants for a lease")
	public ResponseEntity<List<LeaseTenantResponse>> list(@PathVariable UUID leaseId) {
		return ResponseEntity.ok(leaseTenantService.findByLeaseId(leaseId));
	}

	@PostMapping("/invite")
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "Invite tenants to a lease by email")
	public ResponseEntity<List<LeaseTenantResponse>> invite(
			@PathVariable UUID leaseId,
			@Valid @RequestBody InviteLeaseTenantRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		User invitedBy = jwtUserResolver.resolve(jwt);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(leaseTenantService.inviteTenants(leaseId, request, invitedBy));
	}

	@PostMapping("/{leaseTenantId}/resend")
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "Resend the invite for a tenant who hasn't accepted yet")
	public ResponseEntity<LeaseTenantResponse> resendInvite(
			@PathVariable UUID leaseId,
			@PathVariable UUID leaseTenantId) {
		return ResponseEntity.ok(leaseTenantService.resendTenantInvite(leaseId, leaseTenantId));
	}

	@DeleteMapping("/{leaseTenantId}")
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "Remove a tenant from a DRAFT lease")
	public ResponseEntity<Void> remove(
			@PathVariable UUID leaseId,
			@PathVariable UUID leaseTenantId) {
		leaseTenantService.removeTenant(leaseId, leaseTenantId);
		return ResponseEntity.noContent().build();
	}

}
