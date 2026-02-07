package com.akandiah.propmanager.features.unit.api;

import java.util.List;
import java.util.UUID;

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

import com.akandiah.propmanager.features.unit.api.dto.CreateUnitRequest;
import com.akandiah.propmanager.features.unit.api.dto.UnitResponse;
import com.akandiah.propmanager.features.unit.api.dto.UpdateUnitRequest;
import com.akandiah.propmanager.features.unit.service.UnitService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
	@Operation(summary = "List units, optionally by property ID")
	public List<UnitResponse> list(@RequestParam(required = false) UUID propId) {
		if (propId != null)
			return unitService.findByPropId(propId);
		return unitService.findAll();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get unit by ID")
	public UnitResponse getById(@PathVariable UUID id) {
		return unitService.findById(id);
	}

	@PostMapping
	@Operation(summary = "Create a unit")
	public ResponseEntity<UnitResponse> create(@Valid @RequestBody CreateUnitRequest request) {
		UnitResponse created = unitService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@PatchMapping("/{id}")
	@Operation(summary = "Update a unit")
	public UnitResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateUnitRequest request) {
		return unitService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete a unit")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		unitService.deleteById(id);
		return ResponseEntity.noContent().build();
	}
}
