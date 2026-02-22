package com.akandiah.propmanager.features.organization.api;

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
import org.springframework.web.bind.annotation.RestController;

import com.akandiah.propmanager.features.organization.api.dto.CreateMembershipRequest;
import com.akandiah.propmanager.features.organization.api.dto.CreateOrganizationRequest;
import com.akandiah.propmanager.features.organization.api.dto.MembershipResponse;
import com.akandiah.propmanager.features.organization.api.dto.OrganizationResponse;
import com.akandiah.propmanager.features.organization.api.dto.UpdateOrganizationRequest;
import com.akandiah.propmanager.features.organization.service.MembershipService;
import com.akandiah.propmanager.features.organization.service.OrganizationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
@Tag(name = "Organizations", description = "Organization (tenant) CRUD")
public class OrganizationController {

	private final OrganizationService organizationService;
	private final MembershipService membershipService;

	@GetMapping
	@Operation(summary = "List all organizations")
	public ResponseEntity<List<OrganizationResponse>> list() {
		return ResponseEntity.ok(organizationService.findAll());
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get organization by ID")
	public ResponseEntity<OrganizationResponse> getById(@PathVariable UUID id) {
		return ResponseEntity.ok(organizationService.findById(id));
	}

	@PostMapping
	@Operation(summary = "Create an organization")
	public ResponseEntity<OrganizationResponse> create(@Valid @RequestBody CreateOrganizationRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(organizationService.create(request));
	}

	@PatchMapping("/{id}")
	@Operation(summary = "Update an organization")
	public ResponseEntity<OrganizationResponse> update(
			@PathVariable UUID id,
			@Valid @RequestBody UpdateOrganizationRequest request) {
		return ResponseEntity.ok(organizationService.update(id, request));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete an organization")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		organizationService.deleteById(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{id}/members")
	@Operation(summary = "List members of the organization")
	public ResponseEntity<List<MembershipResponse>> listMembers(@PathVariable UUID id) {
		return ResponseEntity.ok(membershipService.findByOrganizationId(id));
	}

	@PostMapping("/{id}/members")
	@Operation(summary = "Add a member to the organization")
	public ResponseEntity<MembershipResponse> addMember(
			@PathVariable UUID id,
			@Valid @RequestBody CreateMembershipRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(membershipService.create(id, request));
	}
}
