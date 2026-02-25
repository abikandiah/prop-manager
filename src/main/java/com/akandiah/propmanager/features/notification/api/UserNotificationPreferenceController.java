package com.akandiah.propmanager.features.notification.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.akandiah.propmanager.features.notification.api.dto.NotificationPreferenceResponse;
import com.akandiah.propmanager.features.notification.api.dto.UpdateNotificationPreferenceRequest;
import com.akandiah.propmanager.features.notification.service.UserNotificationPreferenceService;
import com.akandiah.propmanager.features.user.domain.User;
import com.akandiah.propmanager.security.JwtUserResolver;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notification-preferences")
@RequiredArgsConstructor
@Tag(name = "Notification Preferences", description = "Per-user notification opt-out settings")
public class UserNotificationPreferenceController {

	private final UserNotificationPreferenceService preferenceService;
	private final JwtUserResolver jwtUserResolver;

	@GetMapping
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "List notification preferences for the current user")
	public ResponseEntity<List<NotificationPreferenceResponse>> list(@AuthenticationPrincipal Jwt jwt) {
		User user = jwtUserResolver.resolve(jwt);
		return ResponseEntity.ok(preferenceService.findByUserId(user.getId()));
	}

	@PatchMapping
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "Update a notification preference for the current user")
	public ResponseEntity<NotificationPreferenceResponse> update(
			@Valid @RequestBody UpdateNotificationPreferenceRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		User user = jwtUserResolver.resolve(jwt);
		NotificationPreferenceResponse response = preferenceService.updatePreference(
				user.getId(), request.notificationType(), request.channel(), request.enabled());
		return ResponseEntity.ok(response);
	}
}
