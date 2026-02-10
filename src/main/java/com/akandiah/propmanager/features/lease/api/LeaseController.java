package com.akandiah.propmanager.features.lease.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.akandiah.propmanager.features.lease.api.dto.CreateLeaseRequest;
import com.akandiah.propmanager.features.lease.api.dto.LeaseResponse;
import com.akandiah.propmanager.features.lease.api.dto.UpdateLeaseRequest;
import com.akandiah.propmanager.features.lease.service.LeaseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
	@Operation(summary = "List leases", description = "Optionally filter by ?unitId= or ?propertyId=")
	public List<LeaseResponse> list(
			@RequestParam(required = false) UUID unitId,
			@RequestParam(required = false) UUID propertyId) {
		if (unitId != null) {
			return service.findByUnitId(unitId);
		}
		if (propertyId != null) {
			return service.findByPropertyId(propertyId);
		}
		return service.findAll();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get lease by ID")
	public LeaseResponse getById(@PathVariable UUID id) {
		return service.findById(id);
	}

	// ───────────────────────── Stamp (create) ─────────────────────────

	@PostMapping
	@Operation(summary = "Stamp a new lease from a template", description = "Creates a DRAFT lease with the template's markdown rendered and defaults applied")
	public ResponseEntity<LeaseResponse> create(@Valid @RequestBody CreateLeaseRequest request) {
		LeaseResponse created = service.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	// ───────────────────────── Edit (DRAFT only) ─────────────────────────

	@PatchMapping("/{id}")
	@Operation(summary = "Update a DRAFT lease", description = "Only DRAFT leases can be modified; returns 422 otherwise")
	public LeaseResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateLeaseRequest request) {
		return service.update(id, request);
	}

	// ───────────────────────── Status transitions ─────────────────────────

	@PostMapping("/{id}/submit")
	@Operation(summary = "Submit draft for tenant review", description = "DRAFT → PENDING_REVIEW")
	public LeaseResponse submitForReview(@PathVariable UUID id) {
		return service.submitForReview(id);
	}

	@PostMapping("/{id}/activate")
	@Operation(summary = "Activate a reviewed lease", description = "PENDING_REVIEW → ACTIVE (read-only)")
	public LeaseResponse activate(@PathVariable UUID id) {
		return service.activate(id);
	}

	@PostMapping("/{id}/revert")
	@Operation(summary = "Revert to draft for further edits", description = "PENDING_REVIEW → DRAFT")
	public LeaseResponse revertToDraft(@PathVariable UUID id) {
		return service.revertToDraft(id);
	}

	@PostMapping("/{id}/terminate")
	@Operation(summary = "Terminate an active lease early", description = "ACTIVE → TERMINATED")
	public LeaseResponse terminate(@PathVariable UUID id) {
		return service.terminate(id);
	}

	// ───────────────────────── Delete (DRAFT only) ─────────────────────────

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete a DRAFT lease", description = "Only DRAFT leases can be deleted; returns 422 otherwise")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		service.deleteById(id);
		return ResponseEntity.noContent().build();
	}
}
