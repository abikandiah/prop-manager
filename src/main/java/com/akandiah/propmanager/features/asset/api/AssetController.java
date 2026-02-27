package com.akandiah.propmanager.features.asset.api;

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

import com.akandiah.propmanager.features.asset.api.dto.AssetResponse;
import com.akandiah.propmanager.features.asset.api.dto.CreateAssetRequest;
import com.akandiah.propmanager.features.asset.api.dto.UpdateAssetRequest;
import com.akandiah.propmanager.features.asset.service.AssetService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/assets")
@Tag(name = "Assets", description = "Asset/equipment resource (linked to Prop or Unit)")
public class AssetController {

	private final AssetService assetService;

	public AssetController(AssetService assetService) {
		this.assetService = assetService;
	}

	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "List all assets (admin only — no scope filter)")
	public List<AssetResponse> listAll() {
		return assetService.findAll();
	}

	@GetMapping(params = { "propId", "orgId" })
	@PreAuthorize("@permissionGuard.hasAccess(T(com.akandiah.propmanager.common.permission.Actions).READ, 'm', T(com.akandiah.propmanager.common.permission.ResourceType).PROPERTY, #propId, #orgId)")
	@Operation(summary = "List assets by property ID")
	public List<AssetResponse> listByProp(
			@RequestParam UUID propId,
			@RequestParam UUID orgId) {
		return assetService.findByPropId(propId);
	}

	@GetMapping(params = { "unitId", "orgId" })
	@PreAuthorize("@permissionGuard.hasAccess(T(com.akandiah.propmanager.common.permission.Actions).READ, 'm', T(com.akandiah.propmanager.common.permission.ResourceType).UNIT, #unitId, #orgId)")
	@Operation(summary = "List assets by unit ID")
	public List<AssetResponse> listByUnit(
			@RequestParam UUID unitId,
			@RequestParam UUID orgId) {
		return assetService.findByUnitId(unitId);
	}

	@GetMapping("/{id}")
	@PreAuthorize("@permissionGuard.hasAssetAccess(T(com.akandiah.propmanager.common.permission.Actions).READ, 'm', #id, #orgId)")
	@Operation(summary = "Get asset by ID")
	public AssetResponse getById(@PathVariable UUID id, @RequestParam UUID orgId) {
		return assetService.findById(id);
	}

	@PostMapping
	@PreAuthorize("@permissionGuard.hasAssetCreateAccess(T(com.akandiah.propmanager.common.permission.Actions).CREATE, 'm', #request.propertyId, #request.unitId, #orgId)")
	@Operation(summary = "Create an asset (set exactly one of propertyId or unitId)")
	public ResponseEntity<AssetResponse> create(
			@Valid @RequestBody CreateAssetRequest request,
			@RequestParam UUID orgId) {
		AssetResponse created = assetService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@PatchMapping("/{id}")
	@PreAuthorize("@permissionGuard.hasAssetAccess(T(com.akandiah.propmanager.common.permission.Actions).UPDATE, 'm', #id, #orgId)")
	@Operation(summary = "Update an asset")
	public AssetResponse update(
			@PathVariable UUID id,
			@Valid @RequestBody UpdateAssetRequest request,
			@RequestParam UUID orgId) {
		return assetService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("@permissionGuard.hasAssetAccess(T(com.akandiah.propmanager.common.permission.Actions).DELETE, 'm', #id, #orgId)")
	@Operation(summary = "Delete an asset")
	public ResponseEntity<Void> delete(@PathVariable UUID id, @RequestParam UUID orgId) {
		assetService.deleteById(id);
		return ResponseEntity.noContent().build();
	}
}
