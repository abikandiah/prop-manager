package com.akandiah.propmanager.features.auth.api;

import java.util.List;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.akandiah.propmanager.features.auth.api.dto.RegisterRequest;
import com.akandiah.propmanager.features.auth.api.dto.UserInfoResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import com.akandiah.propmanager.features.user.service.UserService;
import com.akandiah.propmanager.features.user.domain.User;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@Tag(name = "Auth", description = "Current user and auth verification")
@RequiredArgsConstructor
public class AuthController {

	private final UserService userService;

	@GetMapping("/me")
	@Operation(summary = "Get current user info", description = "Returns the authenticated user's sub, name, email and roles. Use for auth verification and UI display.")
	public UserInfoResponse me() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		Jwt jwt = (Jwt) auth.getPrincipal();

		String sub = jwt.getSubject();
		String name = jwt.hasClaim("name") ? jwt.getClaimAsString("name") : null;
		String email = jwt.hasClaim("email") ? jwt.getClaimAsString("email") : null;

		// Update if exists, but do not auto-create
		Optional<User> dbUser = userService.syncUserIfExists(sub, name, email);

		List<String> roles = auth.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.toList();

		String dbRole = dbUser.map(user -> user.getRole().name()).orElse(null);
		String dbStatus = dbUser.map(user -> user.getStatus().name()).orElse(null);

		return new UserInfoResponse(sub, name, email, roles, dbRole, dbStatus);
	}

	@PostMapping("/register")
	@Operation(summary = "Register a new user", description = "Creates a new user record in the local database. Requires valid JWT. Email must be in JWT or provided in body.")
	public UserInfoResponse register(@Valid @RequestBody(required = false) RegisterRequest request) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		Jwt jwt = (Jwt) auth.getPrincipal();

		String sub = jwt.getSubject();
		String name = jwt.hasClaim("name") ? jwt.getClaimAsString("name") : null;
		String email = jwt.hasClaim("email") ? jwt.getClaimAsString("email") : null;

		// If email is not in JWT, take it from request body
		if (email == null && request != null) {
			email = request.email();
		}

		if (email == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required for registration");
		}

		User dbUser = userService.registerUser(sub, name, email);

		List<String> roles = auth.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.toList();

		return new UserInfoResponse(sub, name, email, roles, dbUser.getRole().name(), dbUser.getStatus().name());
	}
}
