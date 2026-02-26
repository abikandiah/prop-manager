package com.akandiah.propmanager.features.membership.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.akandiah.propmanager.features.membership.api.dto.ApplyTemplateRequest;
import com.akandiah.propmanager.features.membership.api.dto.MembershipResponse;
import com.akandiah.propmanager.features.membership.service.MembershipService;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/memberships")
@RequiredArgsConstructor
@Tag(name = "Memberships", description = "User-organization membership management")
public class MembershipController {

	private final MembershipService membershipService;

	@GetMapping
	@Operation(summary = "List memberships by user ID")
	@PreAuthorize("@membershipAuth.canListFor(#userId)")
	public ResponseEntity<List<MembershipResponse>> list(@RequestParam UUID userId) {
		return ResponseEntity.ok(membershipService.findByUserId(userId));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get membership by ID")
	@PreAuthorize("@membershipAuth.canView(#id)")
	public ResponseEntity<MembershipResponse> getById(@PathVariable UUID id) {
		return ResponseEntity.ok(membershipService.findById(id));
	}

	@PostMapping("/{id}/apply-template")
	@Operation(summary = "Apply a template to an existing membership",
		description = "Sets the membership's template and creates binding scope rows for supplied resource IDs. Existing scopes are preserved.")
	@PreAuthorize("@membershipAuth.canManage(#id)")
	public ResponseEntity<MembershipResponse> applyTemplate(
			@PathVariable UUID id,
			@Valid @RequestBody ApplyTemplateRequest request) {
		return ResponseEntity.ok(membershipService.applyTemplate(id, request));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Delete membership (system admin escape hatch)")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		membershipService.deleteById(id);
		return ResponseEntity.noContent().build();
	}
}
