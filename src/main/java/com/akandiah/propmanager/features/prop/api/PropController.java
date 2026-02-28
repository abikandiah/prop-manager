package com.akandiah.propmanager.features.prop.api;

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

import com.akandiah.propmanager.security.annotations.PreAuthorizePropAccess;

import com.akandiah.propmanager.common.permission.AccessListUtil;
import com.akandiah.propmanager.common.permission.AccessListUtil.PropAccessFilter;
import com.akandiah.propmanager.common.permission.Actions;
import com.akandiah.propmanager.common.permission.PermissionDomains;
import com.akandiah.propmanager.features.prop.api.dto.CreatePropRequest;
import com.akandiah.propmanager.features.prop.api.dto.PropResponse;
import com.akandiah.propmanager.features.prop.api.dto.UpdatePropRequest;
import com.akandiah.propmanager.features.prop.service.PropService;
import com.akandiah.propmanager.features.unit.api.dto.UnitResponse;
import com.akandiah.propmanager.features.unit.service.UnitService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/props")
@Tag(name = "Props", description = "Prop resource CRUD")
public class PropController {

	private final PropService propService;
	private final UnitService unitService;

	public PropController(PropService propService, UnitService unitService) {
		this.propService = propService;
		this.unitService = unitService;
	}

	@GetMapping
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "List props visible to the caller")
	public List<PropResponse> list(HttpServletRequest request) {
		PropAccessFilter filter = AccessListUtil.forProps(
				AccessListUtil.fromRequest(request), PermissionDomains.PORTFOLIO, Actions.READ);
		return propService.findAll(filter);
	}

	@GetMapping("/{id}")
	@PreAuthorizePropAccess("READ")
	@Operation(summary = "Get prop by ID")
	public PropResponse getById(@PathVariable UUID id, @RequestParam UUID orgId) {
		return propService.findById(id);
	}

	@GetMapping("/{id}/units")
	@PreAuthorizePropAccess("READ")
	@Operation(summary = "List units for a prop")
	public List<UnitResponse> listUnits(@PathVariable UUID id, @RequestParam UUID orgId) {
		return unitService.findByPropId(id);
	}

	@PostMapping
	@PreAuthorize("@permissionGuard.hasAccess('CREATE', 'PORTFOLIO', 'ORG', #orgId, #orgId)")
	@Operation(summary = "Create a prop")
	public ResponseEntity<PropResponse> create(@Valid @RequestBody CreatePropRequest request,
			@RequestParam UUID orgId) {
		PropResponse created = propService.create(request, orgId);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@PatchMapping("/{id}")
	@PreAuthorizePropAccess("UPDATE")
	@Operation(summary = "Update a prop")
	public PropResponse update(@PathVariable UUID id, @Valid @RequestBody UpdatePropRequest request,
			@RequestParam UUID orgId) {
		return propService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@PreAuthorizePropAccess("DELETE")
	@Operation(summary = "Delete a prop")
	public ResponseEntity<Void> delete(@PathVariable UUID id, @RequestParam UUID orgId) {
		propService.deleteById(id);
		return ResponseEntity.noContent().build();
	}
}