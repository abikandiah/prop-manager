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

import com.akandiah.propmanager.features.lease.api.dto.CreateLeaseTemplateRequest;
import com.akandiah.propmanager.features.lease.api.dto.LeaseTemplateResponse;
import com.akandiah.propmanager.features.lease.api.dto.UpdateLeaseTemplateRequest;
import com.akandiah.propmanager.features.lease.service.LeaseTemplateService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/lease-templates")
@Tag(name = "Lease Templates", description = "Lease template CRUD & versioning")
public class LeaseTemplateController {

	private final LeaseTemplateService service;

	public LeaseTemplateController(LeaseTemplateService service) {
		this.service = service;
	}

	@GetMapping
	@Operation(summary = "List lease templates", description = "Filter with ?active=true for active-only (e.g. for dropdown when creating a lease), or ?search= for name search")
	public List<LeaseTemplateResponse> list(
			@RequestParam(required = false, defaultValue = "false") boolean active,
			@RequestParam(required = false) String search) {
		if (search != null && !search.isBlank()) {
			return service.search(search.strip());
		}
		return active ? service.findActive() : service.findAll();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get lease template by ID")
	public LeaseTemplateResponse getById(@PathVariable UUID id) {
		return service.findById(id);
	}

	@PostMapping
	@Operation(summary = "Create a lease template")
	public ResponseEntity<LeaseTemplateResponse> create(
			@Valid @RequestBody CreateLeaseTemplateRequest request) {
		LeaseTemplateResponse created = service.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@PatchMapping("/{id}")
	@Operation(summary = "Update a lease template", description = "Requires 'version' for optimistic-lock verification; returns 409 if stale")
	public LeaseTemplateResponse update(
			@PathVariable UUID id,
			@Valid @RequestBody UpdateLeaseTemplateRequest request) {
		return service.update(id, request);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete a lease template")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		service.deleteById(id);
		return ResponseEntity.noContent().build();
	}
}
