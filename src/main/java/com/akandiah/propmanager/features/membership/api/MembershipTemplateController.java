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

import com.akandiah.propmanager.features.membership.api.dto.CreateMembershipTemplateRequest;
import com.akandiah.propmanager.features.membership.api.dto.MembershipTemplateResponse;
import com.akandiah.propmanager.features.membership.api.dto.UpdateMembershipTemplateRequest;
import com.akandiah.propmanager.features.membership.service.MembershipTemplateService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/membership-templates")
@Tag(name = "Membership Templates", description = "CRUD for membership templates — system templates readable by any authenticated user; org templates readable by org members; mutations restricted to admins")
public class MembershipTemplateController {

	private final MembershipTemplateService service;

	@GetMapping
	@Operation(summary = "List membership templates by org", description = "Returns system templates plus the given org's own templates")
	@PreAuthorize("hasRole('ADMIN') or @orgGuard.isMember(#orgId, authentication)")
	public ResponseEntity<List<MembershipTemplateResponse>> list(@RequestParam UUID orgId) {
		return ResponseEntity.ok(service.listByOrg(orgId));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get a membership template by ID")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<MembershipTemplateResponse> getById(@PathVariable UUID id) {
		return ResponseEntity.ok(service.findById(id));
	}

	@PostMapping
	@Operation(summary = "Create a membership template")
	@PreAuthorize("hasRole('ADMIN') or " +
			"(#request.orgId() != null and @permissionGuard.hasAccess(" +
			"T(com.akandiah.propmanager.common.permission.Actions).CREATE, 'o', " +
			"T(com.akandiah.propmanager.common.permission.ResourceType).ORG, " +
			"#request.orgId(), #request.orgId()))")
	public ResponseEntity<MembershipTemplateResponse> create(
			@Valid @RequestBody CreateMembershipTemplateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
	}

	@PatchMapping("/{id}")
	@Operation(summary = "Update a membership template", description = "Requires 'version' for optimistic-lock verification. Providing 'items' fully replaces the existing items list.")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<MembershipTemplateResponse> update(
			@PathVariable UUID id,
			@Valid @RequestBody UpdateMembershipTemplateRequest request) {
		return ResponseEntity.ok(service.update(id, request));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete a membership template")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		service.deleteById(id);
		return ResponseEntity.noContent().build();
	}
}
