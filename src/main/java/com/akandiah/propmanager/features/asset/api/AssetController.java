package com.akandiah.propmanager.features.asset.api;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.akandiah.propmanager.common.dto.PageResponse;
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
	@Operation(summary = "List assets, optionally by property ID or unit ID")
	public PageResponse<AssetResponse> list(
			@RequestParam(required = false) UUID propId,
			@RequestParam(required = false) UUID unitId,
			Pageable pageable) {
		if (propId != null) {
			return assetService.findByPropId(propId, pageable);
		}
		if (unitId != null) {
			return assetService.findByUnitId(unitId, pageable);
		}
		return assetService.findAll(pageable);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get asset by ID")
	public AssetResponse getById(@PathVariable UUID id) {
		return assetService.findById(id);
	}

	@PostMapping
	@Operation(summary = "Create an asset (set exactly one of propertyId or unitId)")
	public ResponseEntity<AssetResponse> create(@Valid @RequestBody CreateAssetRequest request) {
		AssetResponse created = assetService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@PatchMapping("/{id}")
	@Operation(summary = "Update an asset")
	public AssetResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateAssetRequest request) {
		return assetService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete an asset")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		assetService.deleteById(id);
		return ResponseEntity.noContent().build();
	}
}
