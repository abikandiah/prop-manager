package com.akandiah.propmanager.features.user.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
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

import com.akandiah.propmanager.features.user.api.dto.PatchMeRequest;
import com.akandiah.propmanager.features.user.api.dto.UserInfoResponse;
import com.akandiah.propmanager.features.user.api.dto.LogoutResponse;
import com.akandiah.propmanager.features.user.domain.User;
import com.akandiah.propmanager.features.user.service.UserService;
import com.akandiah.propmanager.features.organization.service.OrganizationService;

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

		List<String> roles = auth.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.toList();

		var organizations = organizationService.findAll(dbUser.getId());

		return new UserInfoResponse(dbUser.getId(), dbUser.getName(), email, roles, dbUser.getTermsAccepted(),
				organizations);
	}

	@PatchMapping("/me")
	@Operation(summary = "Update current user", description = "Update current user profile (e.g. accept terms).")
	public UserInfoResponse patchMe(
			@AuthenticationPrincipal Jwt jwt,
			Authentication auth,
			@Valid @RequestBody PatchMeRequest request) {
		String issuer = UserService.getIssuerString(jwt);
		String sub = jwt.getSubject();
		String name = jwt.getClaimAsString("name");
		String email = jwt.getClaimAsString("email");

		User dbUser = userService.getOrCreateUser(issuer, sub, name, email);

		if (request.termsAccepted() != null && request.termsAccepted()) {
			dbUser = userService.updateTermsAccepted(dbUser, true);
		}

		List<String> roles = auth.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.toList();

		var organizations = organizationService.findAll(dbUser.getId());

		return new UserInfoResponse(dbUser.getId(), dbUser.getName(), email, roles, dbUser.getTermsAccepted(),
				organizations);
	}

	@PostMapping("/logout")
	@Operation(summary = "Logout user", description = "Returns an optional logout URL for complete session termination (e.g. OIDC redirect).")
	public ResponseEntity<LogoutResponse> logout() {
		return ResponseEntity.ok(new LogoutResponse(logoutUrl));
	}
}
