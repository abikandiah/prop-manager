package com.akandiah.propmanager.features.invite.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.akandiah.propmanager.features.invite.api.dto.CreateInviteRequest;
import com.akandiah.propmanager.features.invite.api.dto.InvitePreviewResponse;
import com.akandiah.propmanager.features.invite.api.dto.InviteResponse;
import com.akandiah.propmanager.features.invite.domain.TargetType;
import com.akandiah.propmanager.features.invite.service.InviteAuthorizationService;
import com.akandiah.propmanager.features.invite.service.InviteService;
import com.akandiah.propmanager.features.user.domain.User;
import com.akandiah.propmanager.security.JwtUserResolver;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller for managing invitations.
 */
@RestController
@Tag(name = "Invites", description = "Invitation management for leases and other resources")
@RequiredArgsConstructor
public class InviteController {

	private final InviteService inviteService;
	private final InviteAuthorizationService inviteAuthService;
	private final JwtUserResolver jwtUserResolver;

	// ───────────────────────── Public (no auth) ─────────────────────────

	@GetMapping("/api/public/invites/{token}")
	@Operation(summary = "Preview an invitation", description = "Returns property, unit, lease and inviter details for the invite link page. Email is masked. No authentication required.")
	public ResponseEntity<InvitePreviewResponse> preview(@PathVariable String token) {
		return ResponseEntity.ok(inviteService.getPreview(token));
	}

	// ───────────────────────── Authenticated commands ─────────────────────────

	@PostMapping("/api/invites")
	@PreAuthorize("@inviteAuthService.canCreateInviteForTarget(#request.targetType(), #request.targetId())")
	@Operation(summary = "Create and send an invitation", description = "Create an invitation. Caller must have CREATE access on the leases domain for the target resource.")
	public ResponseEntity<InviteResponse> createInvite(@Valid @RequestBody CreateInviteRequest request,
			@AuthenticationPrincipal Jwt jwt) {

		User inviter = jwtUserResolver.resolve(jwt);

		InviteResponse response = inviteService.createAndSendInvite(request.email(), request.targetType(),
				request.targetId(), request.attributes(), inviter);

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PostMapping("/api/invites/{token}/accept")
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "Accept an invitation", description = "Accept an invitation. The authenticated user's email must match the invited email.")
	public ResponseEntity<InviteResponse> acceptInvite(@PathVariable String token,
			@AuthenticationPrincipal Jwt jwt) {

		User claimedBy = jwtUserResolver.resolve(jwt);
		return ResponseEntity.ok(inviteService.acceptInvite(token, claimedBy));
	}

	@PostMapping("/api/invites/{id}/resend")
	@PreAuthorize("@inviteAuthService.canManageInvite(#id)")
	@Operation(summary = "Resend an invitation email")
	public ResponseEntity<InviteResponse> resendInvite(@PathVariable UUID id) {
		return ResponseEntity.ok(inviteService.resendInvite(id));
	}

	@DeleteMapping("/api/invites/{id}")
	@PreAuthorize("@inviteAuthService.canManageInvite(#id)")
	@Operation(summary = "Revoke an invitation")
	public ResponseEntity<Void> revokeInvite(@PathVariable UUID id) {
		inviteService.revokeInvite(id);
		return ResponseEntity.noContent().build();
	}

	// ───────────────────────── Queries ─────────────────────────

	@GetMapping("/api/invites/{id}")
	@PreAuthorize("@inviteAuthService.canViewInvite(#id)")
	@Operation(summary = "Get invite by ID")
	public ResponseEntity<InviteResponse> getById(@PathVariable UUID id) {
		return ResponseEntity.ok(inviteService.findById(id));
	}

	@GetMapping("/api/invites")
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "List invitations", description = "Filter by email (own email only, unless ADMIN) or targetType+targetId (requires READ access on the target). Admins may query any email.")
	public ResponseEntity<List<InviteResponse>> listInvites(@RequestParam(required = false) String email,
			@RequestParam(required = false) TargetType targetType, @RequestParam(required = false) UUID targetId,
			@AuthenticationPrincipal Jwt jwt) {

		List<InviteResponse> invites;

		if (email != null) {
			User currentUser = jwtUserResolver.resolve(jwt);
			boolean isAdmin = jwt.getClaimAsStringList("groups") != null
					&& jwt.getClaimAsStringList("groups").contains("ADMIN");
			if (!isAdmin && !currentUser.getEmail().equalsIgnoreCase(email)) {
				throw new AccessDeniedException("Cannot list invites for another user's email");
			}
			invites = inviteService.findInvitesByEmail(email);
		} else if (targetType != null && targetId != null) {
			if (!inviteAuthService.canViewInvitesForTarget(targetType, targetId)) {
				throw new AccessDeniedException("No access to invites for this target");
			}
			invites = inviteService.findInvitesByTarget(targetType, targetId);
		} else {
			throw new IllegalArgumentException("Must provide either email or (targetType and targetId)");
		}

		return ResponseEntity.ok(invites);
	}

}
