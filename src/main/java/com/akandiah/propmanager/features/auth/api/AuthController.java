package com.akandiah.propmanager.features.auth.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.akandiah.propmanager.features.auth.api.dto.UserInfoResponse;
import com.akandiah.propmanager.features.user.domain.User;
import com.akandiah.propmanager.features.user.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@Tag(name = "Auth", description = "Current user and auth verification")
@RequiredArgsConstructor
public class AuthController {

	private final UserService userService;

	@GetMapping("/me")
	@Operation(summary = "Get current user info", description = "Returns the authenticated user's sub, name, email and roles. Use for auth verification and UI display.")
	public UserInfoResponse me(@AuthenticationPrincipal Jwt jwt, Authentication auth) {
		String sub = jwt.getSubject();
		String name = jwt.getClaimAsString("name");
		String email = jwt.getClaimAsString("email");

		// Update if exists, but do not auto-create
		User dbUser = userService.syncUserIfExists(sub, name, email)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not registered"));

		List<String> roles = auth.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.toList();

		return new UserInfoResponse(dbUser.getId(), name, email, roles);
	}

	@PostMapping("/register")
	@Operation(summary = "Register a new user", description = "Creates a new user record in the local database using claims from the authenticated JWT.")
	public UserInfoResponse register(@AuthenticationPrincipal Jwt jwt, Authentication auth) {
		String sub = jwt.getSubject();
		String name = jwt.getClaimAsString("name");
		String email = jwt.getClaimAsString("email");

		if (email == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email claim is missing from JWT");
		}

		User dbUser = userService.registerUser(sub, name, email);

		List<String> roles = auth.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.toList();

		return new UserInfoResponse(dbUser.getId(), name, email, roles);
	}
}
