package com.akandiah.propmanager.features.lease.api;

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

import com.akandiah.propmanager.features.lease.api.dto.CreateLeaseTemplateRequest;
import com.akandiah.propmanager.features.lease.api.dto.LeaseTemplateResponse;
import com.akandiah.propmanager.features.lease.api.dto.UpdateLeaseTemplateRequest;
import com.akandiah.propmanager.features.lease.service.LeaseTemplateService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/lease-templates")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Lease Templates", description = "Lease template CRUD & versioning")
public class LeaseTemplateController {

	private final LeaseTemplateService service;

	public LeaseTemplateController(LeaseTemplateService service) {
		this.service = service;
	}

	@GetMapping
	@Operation(summary = "List lease templates for an org", description = "Filter with ?active=true for active-only, or ?search= for name search")
	public ResponseEntity<List<LeaseTemplateResponse>> list(
			@RequestParam UUID orgId,
			@RequestParam(required = false, defaultValue = "false") boolean active,
			@RequestParam(required = false) String search) {
		if (search != null && !search.isBlank()) {
			return ResponseEntity.ok(service.search(search.strip(), orgId));
		}
		return ResponseEntity.ok(active ? service.findActive(orgId) : service.findAll(orgId));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get lease template by ID")
	public ResponseEntity<LeaseTemplateResponse> getById(@PathVariable UUID id) {
		return ResponseEntity.ok(service.findById(id));
	}

	@PostMapping
	@PreAuthorize("@permissionGuard.hasOrgAccess('CREATE', 'LEASES', #orgId)")
	@Operation(summary = "Create a lease template")
	public ResponseEntity<LeaseTemplateResponse> create(
			@Valid @RequestBody CreateLeaseTemplateRequest request,
			@RequestParam UUID orgId) {
		return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, orgId));
	}

	@PatchMapping("/{id}")
	@PreAuthorize("@permissionGuard.hasOrgAccess('UPDATE', 'LEASES', #orgId)")
	@Operation(summary = "Update a lease template", description = "Requires 'version' for optimistic-lock verification; returns 409 if stale")
	public ResponseEntity<LeaseTemplateResponse> update(
			@PathVariable UUID id,
			@Valid @RequestBody UpdateLeaseTemplateRequest request,
			@RequestParam UUID orgId) {
		return ResponseEntity.ok(service.update(id, request, orgId));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("@permissionGuard.hasOrgAccess('DELETE', 'LEASES', #orgId)")
	@Operation(summary = "Delete a lease template")
	public ResponseEntity<Void> delete(@PathVariable UUID id, @RequestParam UUID orgId) {
		service.deleteById(id, orgId);
		return ResponseEntity.noContent().build();
	}
}