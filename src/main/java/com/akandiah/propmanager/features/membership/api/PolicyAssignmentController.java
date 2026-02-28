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
import org.springframework.web.bind.annotation.RestController;

import com.akandiah.propmanager.features.membership.api.dto.CreatePolicyAssignmentRequest;
import com.akandiah.propmanager.features.membership.api.dto.PolicyAssignmentResponse;
import com.akandiah.propmanager.features.membership.api.dto.UpdatePolicyAssignmentRequest;
import com.akandiah.propmanager.features.membership.service.PolicyAssignmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Policy assignment management for organization members.
 * All endpoints require the caller to be a member of the organization.
 */
@RestController
@RequestMapping("/api/organizations/{orgId}/members/{membershipId}/assignments")
@RequiredArgsConstructor
@Tag(name = "Policy Assignments", description = "Direct policy assignments for organization members")
public class PolicyAssignmentController {

	private final PolicyAssignmentService policyAssignmentService;

	@GetMapping
	@Operation(summary = "List assignments for a membership")
	@PreAuthorize("@membershipAuth.canView(#membershipId, #orgId)")
	public ResponseEntity<List<PolicyAssignmentResponse>> list(
			@PathVariable UUID orgId,
			@PathVariable UUID membershipId) {
		return ResponseEntity.ok(policyAssignmentService.findByMembershipId(membershipId));
	}

	@GetMapping("/{assignmentId}")
	@Operation(summary = "Get an assignment by ID")
	@PreAuthorize("@membershipAuth.canView(#membershipId, #orgId)")
	public ResponseEntity<PolicyAssignmentResponse> getById(
			@PathVariable UUID orgId,
			@PathVariable UUID membershipId,
			@PathVariable UUID assignmentId) {
		return ResponseEntity.ok(policyAssignmentService.findById(membershipId, assignmentId));
	}

	@PostMapping
	@Operation(summary = "Add a policy assignment to a membership")
	@PreAuthorize("@membershipAuth.canManageScopes(2, #orgId, #membershipId)")
	public ResponseEntity<PolicyAssignmentResponse> create(
			@PathVariable UUID orgId,
			@PathVariable UUID membershipId,
			@Valid @RequestBody CreatePolicyAssignmentRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(policyAssignmentService.create(membershipId, request));
	}

	@PatchMapping("/{assignmentId}")
	@Operation(summary = "Update a policy assignment")
	@PreAuthorize("@membershipAuth.canManageScopes(4, #orgId, #membershipId)")
	public ResponseEntity<PolicyAssignmentResponse> update(
			@PathVariable UUID orgId,
			@PathVariable UUID membershipId,
			@PathVariable UUID assignmentId,
			@Valid @RequestBody UpdatePolicyAssignmentRequest request) {
		return ResponseEntity.ok(policyAssignmentService.update(membershipId, assignmentId, request));
	}

	@DeleteMapping("/{assignmentId}")
	@Operation(summary = "Remove a policy assignment from a membership")
	@PreAuthorize("@membershipAuth.canManageScopes(8, #orgId, #membershipId)")
	public ResponseEntity<Void> delete(
			@PathVariable UUID orgId,
			@PathVariable UUID membershipId,
			@PathVariable UUID assignmentId) {
		policyAssignmentService.deleteById(membershipId, assignmentId);
		return ResponseEntity.noContent().build();
	}
}
