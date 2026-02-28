package com.akandiah.propmanager.features.membership.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.akandiah.propmanager.features.membership.api.dto.CreatePermissionPolicyRequest;
import com.akandiah.propmanager.features.membership.api.dto.PermissionPolicyResponse;
import com.akandiah.propmanager.features.membership.api.dto.UpdatePermissionPolicyRequest;
import com.akandiah.propmanager.features.membership.service.PermissionPolicyService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/permission-policies")
@Tag(name = "Permission Policies", description = "CRUD for permission policies — system policies readable by any authenticated user; org policies readable by org members; mutations restricted to admins")
public class PermissionPolicyController {

	private final PermissionPolicyService service;

	@GetMapping
	@Operation(summary = "List permission policies by org", description = "Returns system policies plus the given org's own policies")
	@PreAuthorize("hasRole('ADMIN') or @orgGuard.isMember(#orgId, authentication)")
	public ResponseEntity<List<PermissionPolicyResponse>> list(@RequestParam UUID orgId) {
		return ResponseEntity.ok(service.listByOrg(orgId));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get a permission policy by ID")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<PermissionPolicyResponse> getById(@PathVariable UUID id) {
		return ResponseEntity.ok(service.findById(id));
	}

	@PostMapping
	@Operation(summary = "Create a permission policy")
	@PreAuthorize("hasRole('ADMIN') or " +
			"(@orgGuard.isMember(#orgId, authentication) and @permissionGuard.hasOrgAccess('CREATE', 'ORG', #orgId))")
	public ResponseEntity<PermissionPolicyResponse> create(
			@Valid @RequestBody CreatePermissionPolicyRequest request,
			@RequestParam(required = false) UUID orgId) {
		return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, orgId));
	}

	@PatchMapping("/{id}")
	@Operation(summary = "Update a permission policy", description = "Requires 'version' for optimistic-lock verification.")
	@PreAuthorize("hasRole('ADMIN') or " +
			"(@orgGuard.isMember(#orgId, authentication) and @permissionGuard.hasOrgAccess('UPDATE', 'ORG', #orgId))")
	public ResponseEntity<PermissionPolicyResponse> update(
			@PathVariable UUID id,
			@Valid @RequestBody UpdatePermissionPolicyRequest request,
			@RequestParam(required = false) UUID orgId) {
		return ResponseEntity.ok(service.update(id, request, orgId));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete a permission policy")
	@PreAuthorize("hasRole('ADMIN') or " +
			"(@orgGuard.isMember(#orgId, authentication) and @permissionGuard.hasOrgAccess('DELETE', 'ORG', #orgId))")
	public ResponseEntity<Void> delete(@PathVariable UUID id, @RequestParam(required = false) UUID orgId) {
		service.deleteById(id, orgId);
		return ResponseEntity.noContent().build();
	}
}
