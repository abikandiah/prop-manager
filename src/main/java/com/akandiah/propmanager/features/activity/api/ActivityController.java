package com.akandiah.propmanager.features.activity.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.akandiah.propmanager.common.dto.PageResponse;
import com.akandiah.propmanager.features.activity.api.dto.ActivityEventResponse;
import com.akandiah.propmanager.features.activity.domain.ActivityEventType;
import com.akandiah.propmanager.features.activity.service.ActivityEventService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/activity")
@Tag(name = "Activity", description = "Activity event feed")
@RequiredArgsConstructor
public class ActivityController {

	private final ActivityEventService service;

	@GetMapping
	@PreAuthorize("@permissionGuard.hasAccess('READ', 'PORTFOLIO', 'ORG', #orgId, #orgId)")
	@Operation(summary = "List activity events for an org")
	public ResponseEntity<PageResponse<ActivityEventResponse>> list(
			@RequestParam UUID orgId,
			@RequestParam(required = false) List<ActivityEventType> eventTypes,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ResponseEntity.ok(service.findByOrgId(orgId, eventTypes, page, size));
	}
}
