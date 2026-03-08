package com.akandiah.propmanager.features.user.api;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.akandiah.propmanager.common.permission.AccessEntry;
import com.akandiah.propmanager.common.util.SecurityUtils;
import com.akandiah.propmanager.features.organization.api.dto.UserOrganizationResponse;
import com.akandiah.propmanager.features.organization.service.OrganizationService;
import com.akandiah.propmanager.features.user.api.dto.LogoutResponse;
import com.akandiah.propmanager.features.user.api.dto.PatchMeRequest;
import com.akandiah.propmanager.features.user.api.dto.UiRole;
import com.akandiah.propmanager.features.user.api.dto.UserInfoResponse;
import com.akandiah.propmanager.features.user.domain.User;
import com.akandiah.propmanager.features.user.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@Tag(name = "Current user", description = "Current user profile (me)")
@RequiredArgsConstructor
public class CurrentUserController {

	private final UserService userService;
	private final OrganizationService organizationService;

	@Value("${app.auth.logout-url:#{null}}")
	private String logoutUrl;

	@GetMapping("/me")
	@Operation(summary = "Get current user info", description = "Returns the authenticated user. If no account exists, one is auto-created with termsAccepted=false. Updates last_logged_in_at and name/email from JWT.")
	public UserInfoResponse me(@AuthenticationPrincipal Jwt jwt, Authentication auth) {
		String issuer = UserService.getIssuerString(jwt);
		String sub = jwt.getSubject();
		String name = jwt.getClaimAsString("name");
		String email = jwt.getClaimAsString("email");

		if (email == null || email.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email claim is missing from JWT");
		}

		User dbUser = userService.getOrCreateUser(issuer, sub, name, email);

		List<AccessEntry> access = SecurityUtils.getAccessFromRequest();
		return toResponse(dbUser, auth, access);
	}

	@PatchMapping("/me")
	@Operation(summary = "Update current user", description = "Update current user profile (name, phone, terms).")
	public UserInfoResponse patchMe(
			@AuthenticationPrincipal Jwt jwt,
			Authentication auth,
			@Valid @RequestBody PatchMeRequest request) {
		User dbUser = userService.findUserFromJwt(jwt)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

		if (request.termsAccepted() != null && request.termsAccepted()) {
			dbUser = userService.updateTermsAccepted(dbUser, true);
		}
		if (request.name() != null || request.phoneNumber() != null) {
			dbUser = userService.updateProfile(dbUser, request.name(), request.phoneNumber());
		}

		List<AccessEntry> access = SecurityUtils.getAccessFromRequest();
		return toResponse(dbUser, auth, access);
	}

	@PostMapping("/logout")
	@Operation(summary = "Logout user", description = "Returns an optional logout URL for complete session termination (e.g. OIDC redirect).")
	public ResponseEntity<LogoutResponse> logout() {
		return ResponseEntity.ok(new LogoutResponse(logoutUrl));
	}

	private UserInfoResponse toResponse(User dbUser, Authentication auth, List<AccessEntry> access) {
		List<String> authorities = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
		List<UserOrganizationResponse> organizations = buildOrgResponses(dbUser.getId(), authorities, access);
		List<UiRole> uiRoles = userService.deriveRoles(authorities, access);
		return new UserInfoResponse(dbUser.getId(), dbUser.getName(), dbUser.getEmail(),
				dbUser.getPhoneNumber(), dbUser.getTermsAccepted(), organizations, uiRoles, access);
	}

	/** Builds org responses with per-org roles derived from the filtered access list. */
	private List<UserOrganizationResponse> buildOrgResponses(UUID userId, List<String> authorities, List<AccessEntry> access) {
		return organizationService.findAllSimplified(userId).stream()
				.map(org -> {
					List<AccessEntry> orgAccess = access.stream()
							.filter(e -> org.getId().equals(e.orgId()))
							.toList();
					List<UiRole> orgRoles = userService.deriveRoles(authorities, orgAccess);
					return UserOrganizationResponse.from(org, orgRoles);
				})
				.toList();
	}
}
