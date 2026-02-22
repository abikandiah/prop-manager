package com.akandiah.propmanager.features.notification.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.akandiah.propmanager.features.notification.api.dto.NotificationDeliveryResponse;
import com.akandiah.propmanager.features.notification.service.NotificationDeliveryService;
import com.akandiah.propmanager.features.user.domain.User;
import com.akandiah.propmanager.features.user.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "View and manage notification deliveries")
public class NotificationDeliveryController {

	private final NotificationDeliveryService deliveryService;
	private final UserService userService;

	@GetMapping
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "List notifications for the current user",
			security = @SecurityRequirement(name = "bearer-jwt"))
	public ResponseEntity<List<NotificationDeliveryResponse>> list(@AuthenticationPrincipal Jwt jwt) {
		User user = getCurrentUser(jwt);
		return ResponseEntity.ok(deliveryService.findByUserId(user.getId()));
	}

	@PatchMapping("/{id}/viewed")
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "Mark a notification as viewed",
			security = @SecurityRequirement(name = "bearer-jwt"))
	public ResponseEntity<Void> markViewed(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
		deliveryService.markViewed(id);
		return ResponseEntity.noContent().build();
	}

	private User getCurrentUser(Jwt jwt) {
		return userService.findUserFromJwt(jwt)
				.orElseThrow(() -> new IllegalStateException("User not found for authenticated subject"));
	}
}
