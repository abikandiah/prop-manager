package com.akandiah.propmanager.features.permission.api;

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

import com.akandiah.propmanager.features.permission.api.dto.CreatePermissionTemplateRequest;
import com.akandiah.propmanager.features.permission.api.dto.PermissionTemplateResponse;
import com.akandiah.propmanager.features.permission.api.dto.UpdatePermissionTemplateRequest;
import com.akandiah.propmanager.features.permission.service.PermissionTemplateService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/permission-templates")
@Tag(name = "Permission Templates", description = "CRUD for permission templates — system templates readable by any authenticated user; org templates readable by org members; mutations restricted to admins")
public class PermissionTemplateController {

	private final PermissionTemplateService service;

	@GetMapping
	@Operation(summary = "List permission templates by org", description = "Returns system templates (org_id null) plus the given org's templates")
	@PreAuthorize("hasRole('ADMIN') or @orgGuard.isMember(#orgId, authentication)")
	public ResponseEntity<List<PermissionTemplateResponse>> list(@RequestParam UUID orgId) {
		return ResponseEntity.ok(service.listByOrg(orgId));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get a permission template by ID")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<PermissionTemplateResponse> getById(@PathVariable UUID id) {
		return ResponseEntity.ok(service.findById(id));
	}

	@PostMapping
	@Operation(summary = "Create a permission template")
	@PreAuthorize("hasRole('ADMIN') or " +
			"(#request.orgId() != null and @permissionGuard.hasAccess(" +
			"T(com.akandiah.propmanager.common.permission.Actions).CREATE, 'o', " +
			"T(com.akandiah.propmanager.common.permission.ResourceType).ORG, " +
			"#request.orgId(), #request.orgId()))")
	public ResponseEntity<PermissionTemplateResponse> create(
			@Valid @RequestBody CreatePermissionTemplateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
	}

	@PatchMapping("/{id}")
	@Operation(summary = "Update a permission template", description = "Requires 'version' for optimistic-lock verification")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<PermissionTemplateResponse> update(
			@PathVariable UUID id,
			@Valid @RequestBody UpdatePermissionTemplateRequest request) {
		return ResponseEntity.ok(service.update(id, request));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete a permission template")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		service.deleteById(id);
		return ResponseEntity.noContent().build();
	}
}
