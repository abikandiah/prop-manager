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
import org.springframework.web.bind.annotation.RestController;

import com.akandiah.propmanager.features.organization.api.dto.CreateMemberScopeRequest;
import com.akandiah.propmanager.features.organization.api.dto.MemberScopeResponse;
import com.akandiah.propmanager.features.organization.api.dto.UpdateMemberScopeRequest;
import com.akandiah.propmanager.features.organization.service.MemberScopeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Granular permission scope management for organization members.
 * All endpoints require the caller to be a member of the organization.
 */
@RestController
@RequestMapping("/api/organizations/{orgId}/members/{membershipId}/scopes")
@RequiredArgsConstructor
@Tag(name = "Member Scopes", description = "Granular permission scopes for organization members")
public class MemberScopeController {

	private final MemberScopeService memberScopeService;

	@GetMapping
	@Operation(summary = "List scopes for a membership")
	@PreAuthorize("hasRole('ADMIN') or @orgAuthz.isMember(#orgId, authentication)")
	public ResponseEntity<List<MemberScopeResponse>> list(
			@PathVariable UUID orgId,
			@PathVariable UUID membershipId) {
		return ResponseEntity.ok(memberScopeService.findByMembershipId(membershipId));
	}

	@GetMapping("/{scopeId}")
	@Operation(summary = "Get a scope by ID")
	@PreAuthorize("hasRole('ADMIN') or @orgAuthz.isMember(#orgId, authentication)")
	public ResponseEntity<MemberScopeResponse> getById(
			@PathVariable UUID orgId,
			@PathVariable UUID membershipId,
			@PathVariable UUID scopeId) {
		return ResponseEntity.ok(memberScopeService.findById(membershipId, scopeId));
	}

	@PostMapping
	@Operation(summary = "Add a scope to a membership")
	@PreAuthorize("hasRole('ADMIN') or @orgAuthz.isMember(#orgId, authentication)")
	public ResponseEntity<MemberScopeResponse> create(
			@PathVariable UUID orgId,
			@PathVariable UUID membershipId,
			@Valid @RequestBody CreateMemberScopeRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(memberScopeService.create(membershipId, request));
	}

	@PatchMapping("/{scopeId}")
	@Operation(summary = "Update a scope's permissions")
	@PreAuthorize("hasRole('ADMIN') or @orgAuthz.isMember(#orgId, authentication)")
	public ResponseEntity<MemberScopeResponse> update(
			@PathVariable UUID orgId,
			@PathVariable UUID membershipId,
			@PathVariable UUID scopeId,
			@Valid @RequestBody UpdateMemberScopeRequest request) {
		return ResponseEntity.ok(memberScopeService.update(membershipId, scopeId, request));
	}

	@DeleteMapping("/{scopeId}")
	@Operation(summary = "Remove a scope from a membership")
	@PreAuthorize("hasRole('ADMIN') or @orgAuthz.isMember(#orgId, authentication)")
	public ResponseEntity<Void> delete(
			@PathVariable UUID orgId,
			@PathVariable UUID membershipId,
			@PathVariable UUID scopeId) {
		memberScopeService.deleteById(membershipId, scopeId);
		return ResponseEntity.noContent().build();
	}
}
