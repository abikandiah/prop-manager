package com.akandiah.propmanager.features.unit.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.akandiah.propmanager.common.permission.AccessListUtil;
import com.akandiah.propmanager.common.permission.AccessListUtil.ScopedAccessFilter;
import com.akandiah.propmanager.common.permission.Actions;
import com.akandiah.propmanager.common.permission.PermissionDomains;
import com.akandiah.propmanager.features.unit.api.dto.CreateUnitRequest;
import com.akandiah.propmanager.features.unit.api.dto.UnitResponse;
import com.akandiah.propmanager.features.unit.api.dto.UpdateUnitRequest;
import com.akandiah.propmanager.features.unit.service.UnitService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/units")
@Tag(name = "Units", description = "Unit resource CRUD (child of Prop)")
public class UnitController {

	private final UnitService unitService;

	public UnitController(UnitService unitService) {
		this.unitService = unitService;
	}

	@GetMapping
	@PreAuthorize("@orgGuard.isMember(#orgId, authentication)")
	@Operation(summary = "List units (optionally filtered by propId, or all accessible in organization)")
	public List<UnitResponse> list(
			@RequestParam(required = false) UUID propId,
			@RequestParam UUID orgId,
			HttpServletRequest request) {
		ScopedAccessFilter filter = AccessListUtil.forScopedResources(
				AccessListUtil.fromRequest(request), PermissionDomains.PORTFOLIO, Actions.READ);
		return unitService.findAll(filter, orgId, propId);
	}

	@GetMapping("/{id}")
	@PreAuthorize("@permissionGuard.hasAccess(T(com.akandiah.propmanager.common.permission.Actions).READ, 'p', T(com.akandiah.propmanager.common.permission.ResourceType).UNIT, #id, #orgId)")
	@Operation(summary = "Get unit by ID")
	public UnitResponse getById(@PathVariable UUID id, @RequestParam UUID orgId) {
		return unitService.findById(id);
	}

	@PostMapping
	@PreAuthorize("@permissionGuard.hasAccess(T(com.akandiah.propmanager.common.permission.Actions).CREATE, 'p', T(com.akandiah.propmanager.common.permission.ResourceType).PROPERTY, #request.propertyId, #orgId)")
	@Operation(summary = "Create a unit")
	public ResponseEntity<UnitResponse> create(@Valid @RequestBody CreateUnitRequest request,
			@RequestParam UUID orgId) {
		UnitResponse created = unitService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@PatchMapping("/{id}")
	@PreAuthorize("@permissionGuard.hasAccess(T(com.akandiah.propmanager.common.permission.Actions).UPDATE, 'p', T(com.akandiah.propmanager.common.permission.ResourceType).UNIT, #id, #orgId)")
	@Operation(summary = "Update a unit")
	public UnitResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateUnitRequest request,
			@RequestParam UUID orgId) {
		return unitService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("@permissionGuard.hasAccess(T(com.akandiah.propmanager.common.permission.Actions).DELETE, 'p', T(com.akandiah.propmanager.common.permission.ResourceType).UNIT, #id, #orgId)")
	@Operation(summary = "Delete a unit")
	public ResponseEntity<Void> delete(@PathVariable UUID id, @RequestParam UUID orgId) {
		unitService.deleteById(id);
		return ResponseEntity.noContent().build();
	}
}
