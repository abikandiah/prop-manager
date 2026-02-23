package com.akandiah.propmanager.features.organization.api;

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

import com.akandiah.propmanager.features.organization.api.dto.CreateMemberScopeRequest;
import com.akandiah.propmanager.features.organization.api.dto.MemberScopeResponse;
import com.akandiah.propmanager.features.organization.api.dto.MembershipResponse;
import com.akandiah.propmanager.features.organization.api.dto.UpdateMembershipRequest;
import com.akandiah.propmanager.features.organization.service.MemberScopeService;
import com.akandiah.propmanager.features.organization.service.MembershipService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/memberships")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
@Tag(name = "Memberships", description = "User-organization membership and granular scopes")
public class MembershipController {

	private final MembershipService membershipService;
	private final MemberScopeService memberScopeService;

	@GetMapping
	@Operation(summary = "List memberships by user ID")
	public ResponseEntity<List<MembershipResponse>> list(@RequestParam UUID userId) {
		return ResponseEntity.ok(membershipService.findByUserId(userId));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get membership by ID")
	public ResponseEntity<MembershipResponse> getById(@PathVariable UUID id) {
		return ResponseEntity.ok(membershipService.findById(id));
	}

	@PatchMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Update membership role")
	public ResponseEntity<MembershipResponse> update(
			@PathVariable UUID id,
			@Valid @RequestBody UpdateMembershipRequest request) {
		return ResponseEntity.ok(membershipService.update(id, request));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Delete membership")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		membershipService.deleteById(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{id}/scopes")
	@Operation(summary = "List scopes for a membership")
	public ResponseEntity<List<MemberScopeResponse>> listScopes(@PathVariable UUID id) {
		return ResponseEntity.ok(memberScopeService.findByMembershipId(id));
	}

	@PostMapping("/{id}/scopes")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Add a scope to a membership")
	public ResponseEntity<MemberScopeResponse> addScope(
			@PathVariable UUID id,
			@Valid @RequestBody CreateMemberScopeRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(memberScopeService.create(id, request));
	}

	@DeleteMapping("/{id}/scopes/{scopeId}")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Remove a scope from a membership")
	public ResponseEntity<Void> removeScope(@PathVariable UUID id, @PathVariable UUID scopeId) {
		memberScopeService.deleteById(id, scopeId);
		return ResponseEntity.noContent().build();
	}
}
