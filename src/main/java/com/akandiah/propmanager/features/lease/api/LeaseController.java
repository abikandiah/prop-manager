package com.akandiah.propmanager.features.lease.api;

import java.util.List;
import java.util.Set;
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

import com.akandiah.propmanager.common.permission.AccessListUtil;
import com.akandiah.propmanager.common.permission.AccessListUtil.LeaseAccessFilter;
import com.akandiah.propmanager.common.permission.Actions;
import com.akandiah.propmanager.common.permission.PermissionDomains;
import com.akandiah.propmanager.features.lease.api.dto.CreateLeaseRequest;
import com.akandiah.propmanager.features.lease.api.dto.LeaseResponse;
import com.akandiah.propmanager.features.lease.api.dto.UpdateLeaseRequest;
import com.akandiah.propmanager.features.lease.service.LeaseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/leases")
@Tag(name = "Leases", description = "Lease lifecycle: stamp, edit, review, activate")
public class LeaseController {

	private final LeaseService service;

	public LeaseController(LeaseService service) {
		this.service = service;
	}

	// ───────────────────────── Queries ─────────────────────────

	@GetMapping
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "List leases", description = "Optionally filter by ?unitId= or ?propertyId=. Results are restricted to leases the caller is authorized to see.")
	public ResponseEntity<List<LeaseResponse>> list(
			@RequestParam(required = false) UUID unitId,
			@RequestParam(required = false) UUID propertyId,
			HttpServletRequest request) {
		LeaseAccessFilter baseFilter = AccessListUtil.forLeases(
				AccessListUtil.fromRequest(request), PermissionDomains.LEASES, Actions.READ);
		if (unitId != null) {
			// Intersect base filter with the requested unitId
			LeaseAccessFilter unitFilter = new LeaseAccessFilter(
					baseFilter.orgIds(), baseFilter.propIds(), Set.of(unitId));
			return ResponseEntity.ok(service.findAll(unitFilter));
		}
		if (propertyId != null) {
			LeaseAccessFilter propFilter = new LeaseAccessFilter(
					baseFilter.orgIds(), Set.of(propertyId), baseFilter.unitIds());
			return ResponseEntity.ok(service.findAll(propFilter));
		}
		return ResponseEntity.ok(service.findAll(baseFilter));
	}

	@GetMapping("/{id}")
	@PreAuthorize("@permissionAuth.hasLeaseAccess(T(com.akandiah.propmanager.common.permission.Actions).READ, 'l', #id, #orgId)")
	@Operation(summary = "Get lease by ID")
	public ResponseEntity<LeaseResponse> getById(@PathVariable UUID id, @RequestParam UUID orgId) {
		return ResponseEntity.ok(service.findById(id));
	}

	// ───────────────────────── Stamp (create) ─────────────────────────

	@PostMapping
	@PreAuthorize("@permissionAuth.hasAccess(T(com.akandiah.propmanager.common.permission.Actions).CREATE, 'l', T(com.akandiah.propmanager.common.permission.ResourceType).UNIT, #request.unitId, #orgId)")
	@Operation(summary = "Stamp a new lease from a template", description = "Creates a DRAFT lease with template defaults applied; markdown is rendered on activate")
	public ResponseEntity<LeaseResponse> create(@Valid @RequestBody CreateLeaseRequest request,
			@RequestParam UUID orgId) {
		return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
	}

	// ───────────────────────── Edit (DRAFT only) ─────────────────────────

	@PatchMapping("/{id}")
	@PreAuthorize("@permissionAuth.hasLeaseAccess(T(com.akandiah.propmanager.common.permission.Actions).UPDATE, 'l', #id, #orgId)")
	@Operation(summary = "Update a DRAFT lease", description = "Only DRAFT leases can be modified; returns 422 otherwise")
	public ResponseEntity<LeaseResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateLeaseRequest request,
			@RequestParam UUID orgId) {
		return ResponseEntity.ok(service.update(id, request));
	}

	// ───────────────────────── Status transitions ─────────────────────────

	@PostMapping("/{id}/submit")
	@PreAuthorize("@permissionAuth.hasLeaseAccess(T(com.akandiah.propmanager.common.permission.Actions).UPDATE, 'l', #id, #orgId)")
	@Operation(summary = "Submit draft for tenant review", description = "DRAFT → REVIEW")
	public ResponseEntity<LeaseResponse> submitForReview(@PathVariable UUID id, @RequestParam UUID orgId) {
		return ResponseEntity.ok(service.submitForReview(id));
	}

	@PostMapping("/{id}/activate")
	@PreAuthorize("@permissionAuth.hasLeaseAccess(T(com.akandiah.propmanager.common.permission.Actions).UPDATE, 'l', #id, #orgId)")
	@Operation(summary = "Activate a reviewed lease", description = "REVIEW → ACTIVE (read-only); stamps template markdown")
	public ResponseEntity<LeaseResponse> activate(@PathVariable UUID id, @RequestParam UUID orgId) {
		return ResponseEntity.ok(service.activate(id));
	}

	@PostMapping("/{id}/revert")
	@PreAuthorize("@permissionAuth.hasLeaseAccess(T(com.akandiah.propmanager.common.permission.Actions).UPDATE, 'l', #id, #orgId)")
	@Operation(summary = "Revert to draft for further edits", description = "REVIEW → DRAFT")
	public ResponseEntity<LeaseResponse> revertToDraft(@PathVariable UUID id, @RequestParam UUID orgId) {
		return ResponseEntity.ok(service.revertToDraft(id));
	}

	@PostMapping("/{id}/terminate")
	@PreAuthorize("@permissionAuth.hasLeaseAccess(T(com.akandiah.propmanager.common.permission.Actions).UPDATE, 'l', #id, #orgId)")
	@Operation(summary = "Terminate an active lease early", description = "ACTIVE → TERMINATED")
	public ResponseEntity<LeaseResponse> terminate(@PathVariable UUID id, @RequestParam UUID orgId) {
		return ResponseEntity.ok(service.terminate(id));
	}

	// ───────────────────────── Delete (DRAFT only) ─────────────────────────

	@DeleteMapping("/{id}")
	@PreAuthorize("@permissionAuth.hasLeaseAccess(T(com.akandiah.propmanager.common.permission.Actions).DELETE, 'l', #id, #orgId)")
	@Operation(summary = "Delete a DRAFT lease", description = "Only DRAFT leases can be deleted; returns 422 otherwise")
	public ResponseEntity<Void> delete(@PathVariable UUID id, @RequestParam UUID orgId) {
		service.deleteById(id);
		return ResponseEntity.noContent().build();
	}
}
