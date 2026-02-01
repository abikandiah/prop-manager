package com.akandiah.propmanager.features.auth.api;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.akandiah.propmanager.features.auth.api.dto.UserInfoResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api")
@Tag(name = "Auth", description = "Current user and auth verification")
public class AuthController {

	@GetMapping("/me")
	@Operation(summary = "Get current user info", description = "Returns the authenticated user's sub, name, email and roles. Use for auth verification and UI display.")
	public UserInfoResponse me() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		Jwt jwt = (Jwt) auth.getPrincipal();

		String sub = jwt.getSubject();
		String name = jwt.hasClaim("name") ? jwt.getClaimAsString("name") : null;
		String email = jwt.hasClaim("email") ? jwt.getClaimAsString("email") : null;
		List<String> roles = auth.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.toList();

		return new UserInfoResponse(sub, name, email, roles);
	}
}
