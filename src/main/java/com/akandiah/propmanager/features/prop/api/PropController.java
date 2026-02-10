package com.akandiah.propmanager.features.prop.api;

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
import org.springframework.web.bind.annotation.RestController;

import com.akandiah.propmanager.common.dto.PageResponse;
import com.akandiah.propmanager.features.prop.api.dto.CreatePropRequest;
import com.akandiah.propmanager.features.prop.api.dto.PropResponse;
import com.akandiah.propmanager.features.prop.api.dto.UpdatePropRequest;
import com.akandiah.propmanager.features.prop.service.PropService;
import com.akandiah.propmanager.features.unit.api.dto.UnitResponse;
import com.akandiah.propmanager.features.unit.service.UnitService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
	@Operation(summary = "List all props")
	public PageResponse<PropResponse> list(Pageable pageable) {
		return propService.findAll(pageable);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get prop by ID")
	public PropResponse getById(@PathVariable UUID id) {
		return propService.findById(id);
	}

	@GetMapping("/{id}/units")
	@Operation(summary = "List units for a prop")
	public PageResponse<UnitResponse> listUnits(@PathVariable UUID id, Pageable pageable) {
		return unitService.findByPropId(id, pageable);
	}

	@PostMapping
	@Operation(summary = "Create a prop")
	public ResponseEntity<PropResponse> create(@Valid @RequestBody CreatePropRequest request) {
		PropResponse created = propService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@PatchMapping("/{id}")
	@Operation(summary = "Update a prop")
	public PropResponse update(@PathVariable UUID id, @Valid @RequestBody UpdatePropRequest request) {
		return propService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete a prop")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		propService.deleteById(id);
		return ResponseEntity.noContent().build();
	}
}
