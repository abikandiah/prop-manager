package com.akandiah.propmanager.features.organization.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.akandiah.propmanager.features.organization.api.dto.CreateOrganizationRequest;
import com.akandiah.propmanager.features.membership.api.dto.InviteMemberRequest;
import com.akandiah.propmanager.features.membership.api.dto.MembershipResponse;
import com.akandiah.propmanager.features.organization.api.dto.OrganizationResponse;
import com.akandiah.propmanager.features.organization.api.dto.UpdateOrganizationRequest;
import com.akandiah.propmanager.features.membership.service.MembershipService;
import com.akandiah.propmanager.features.organization.service.OrganizationService;
import com.akandiah.propmanager.features.user.domain.User;
import com.akandiah.propmanager.security.JwtUserResolver;

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
	private final JwtUserResolver jwtUserResolver;

	/** Lists organizations visible to the caller: all orgs for ADMIN, own memberships for everyone else. */
	@GetMapping
	@Operation(summary = "List organizations")
	public ResponseEntity<List<OrganizationResponse>> list(Authentication authentication) {
		boolean isAdmin = authentication.getAuthorities().stream()
				.anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
		if (isAdmin) {
			return ResponseEntity.ok(organizationService.findAllForAdmin());
		}
		UUID userId = jwtUserResolver.resolve().getId();
		return ResponseEntity.ok(organizationService.findAll(userId));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get organization by ID")
	@PreAuthorize("hasRole('ADMIN') or @orgGuard.isMember(#id, authentication)")
	public ResponseEntity<OrganizationResponse> getById(@PathVariable UUID id) {
		return ResponseEntity.ok(organizationService.findById(id));
	}

	/** Any authenticated user may create an organization. The creator is auto-enrolled as org admin. */
	@PostMapping
	@Operation(summary = "Create an organization")
	public ResponseEntity<OrganizationResponse> create(@Valid @RequestBody CreateOrganizationRequest request) {
		UUID creatorId = jwtUserResolver.resolve().getId();
		return ResponseEntity.status(HttpStatus.CREATED).body(organizationService.create(request, creatorId));
	}

	@PatchMapping("/{id}")
	@Operation(summary = "Update an organization")
	@PreAuthorize("@permissionGuard.hasOrgAccess(4, 'o', #id)")
	public ResponseEntity<OrganizationResponse> update(
			@PathVariable UUID id,
			@Valid @RequestBody UpdateOrganizationRequest request) {
		return ResponseEntity.ok(organizationService.update(id, request));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete an organization")
	@PreAuthorize("@permissionGuard.hasOrgAccess(8, 'o', #id)")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		organizationService.deleteById(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{id}/members")
	@Operation(summary = "List members of the organization")
	@PreAuthorize("hasRole('ADMIN') or @orgGuard.isMember(#id, authentication)")
	public ResponseEntity<List<MembershipResponse>> listMembers(@PathVariable UUID id) {
		return ResponseEntity.ok(membershipService.findByOrganizationId(id));
	}

	@PostMapping("/{id}/members/invites")
	@Operation(summary = "Invite a member to the organization")
	@PreAuthorize("@inviteAuthService.canCreateInviteForTarget(T(com.akandiah.propmanager.features.invite.domain.TargetType).MEMBERSHIP, #id)")
	public ResponseEntity<MembershipResponse> inviteMember(
			@PathVariable UUID id,
			@Valid @RequestBody InviteMemberRequest request) {
		User invitedBy = jwtUserResolver.resolve();
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(membershipService.inviteMember(id, request.email(), request.templateId(), request.initialScopes(), invitedBy));
	}

	@DeleteMapping("/{id}/members/{membershipId}")
	@Operation(summary = "Remove a member from the organization")
	@PreAuthorize("@permissionGuard.hasOrgAccess(8, 'o', #id)")
	public ResponseEntity<Void> deleteMember(
			@PathVariable UUID id,
			@PathVariable UUID membershipId) {
		membershipService.deleteById(id, membershipId);
		return ResponseEntity.noContent().build();
	}

}
