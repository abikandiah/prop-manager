package com.akandiah.propmanager.features.invite.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.akandiah.propmanager.features.invite.api.dto.CreateInviteRequest;
import com.akandiah.propmanager.features.invite.api.dto.InviteResponse;
import com.akandiah.propmanager.features.invite.api.dto.RedeemInviteRequest;
import com.akandiah.propmanager.features.invite.domain.TargetType;
import com.akandiah.propmanager.features.invite.service.InviteService;
import com.akandiah.propmanager.features.user.domain.User;
import com.akandiah.propmanager.features.user.domain.UserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller for managing invitations.
 */
@RestController
@RequestMapping("/api/invites")
@Tag(name = "Invites", description = "Invitation management for leases, properties, and other resources")
@RequiredArgsConstructor
public class InviteController {

	private final InviteService inviteService;
	private final UserRepository userRepository;

	// ───────────────────────── Commands ─────────────────────────

	@PostMapping
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "Create and send an invitation", security = @SecurityRequirement(name = "bearer-jwt"), description = "Create an invitation. User must be authenticated. TODO: Add permission check to verify user owns/manages the target resource.")
	public ResponseEntity<InviteResponse> createInvite(@Valid @RequestBody CreateInviteRequest request,
			@AuthenticationPrincipal Jwt jwt) {

		// TODO: Verify user has permission to invite to the target resource
		// e.g., check if user owns the property/lease before allowing invite creation

		User inviter = getCurrentUser(jwt);

		InviteResponse response = inviteService.createAndSendInvite(request.email(), request.targetType(), request.targetId(),
				request.role(), inviter, request.metadata());

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PostMapping("/{id}/resend")
	@PreAuthorize("@inviteAuthService.canManageInvite(#id)")
	@Operation(summary = "Resend an invitation email", security = @SecurityRequirement(name = "bearer-jwt"))
	public ResponseEntity<InviteResponse> resendInvite(@PathVariable UUID id,
			@RequestBody(required = false) Map<String, Object> metadata) {

		return ResponseEntity.ok(inviteService.resendInvite(id, metadata));
	}

	@PostMapping("/redeem")
	@Operation(summary = "Redeem an invitation (public endpoint)", description = "Accept an invitation and create/link user account. No authentication required.")
	public ResponseEntity<InviteResponse> redeemInvite(@Valid @RequestBody RedeemInviteRequest request) {

		// TODO: Create or find user account based on email
		// For now, this is a placeholder - you'll need to implement user creation logic
		User user = findOrCreateUser(request);

		return ResponseEntity.ok(inviteService.redeemInvite(request.token(), user));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("@inviteAuthService.canManageInvite(#id)")
	@Operation(summary = "Revoke an invitation", security = @SecurityRequirement(name = "bearer-jwt"))
	public ResponseEntity<Void> revokeInvite(@PathVariable UUID id) {
		inviteService.revokeInvite(id);
		return ResponseEntity.noContent().build();
	}

	// ───────────────────────── Queries ─────────────────────────

	@GetMapping("/{id}")
	@PreAuthorize("@inviteAuthService.canViewInvite(#id)")
	@Operation(summary = "Get invite by ID", security = @SecurityRequirement(name = "bearer-jwt"))
	public ResponseEntity<InviteResponse> getById(@PathVariable UUID id) {
		return ResponseEntity.ok(inviteService.findById(id));
	}

	@GetMapping
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "List invitations", description = "Filter by email, targetType, or targetId. Results are filtered based on user permissions.", security = @SecurityRequirement(name = "bearer-jwt"))
	public ResponseEntity<List<InviteResponse>> listInvites(@RequestParam(required = false) String email,
			@RequestParam(required = false) TargetType targetType, @RequestParam(required = false) UUID targetId,
			@AuthenticationPrincipal Jwt jwt) {

		List<InviteResponse> invites;

		if (email != null) {
			invites = inviteService.findInvitesByEmail(email);
		} else if (targetType != null && targetId != null) {
			invites = inviteService.findInvitesByTarget(targetType, targetId);
		} else {
			throw new IllegalArgumentException("Must provide either email or (targetType and targetId)");
		}

		// TODO: Filter results based on user permissions
		// Users should only see invites they created, received, or for resources they manage

		return ResponseEntity.ok(invites);
	}

	// ───────────────────────── Helpers ─────────────────────────

	private User getCurrentUser(Jwt jwt) {
		String userSub = jwt.getSubject();
		return userRepository.findByIdpSub(userSub)
				.orElseThrow(() -> new IllegalStateException("User not found for authenticated subject"));
	}

	private User findOrCreateUser(RedeemInviteRequest request) {
		// TODO: Implement proper user creation/lookup logic
		// This is a placeholder that needs to be replaced with actual user management
		throw new UnsupportedOperationException(
				"User creation not yet implemented. Integrate with your user management system.");
	}
}
