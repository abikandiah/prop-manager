package com.akandiah.propmanager.features.prop.api;

import java.util.List;

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

import com.akandiah.propmanager.features.prop.api.dto.CreatePropRequest;
import com.akandiah.propmanager.features.prop.api.dto.PropResponse;
import com.akandiah.propmanager.features.prop.api.dto.UpdatePropRequest;
import com.akandiah.propmanager.features.prop.service.PropService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/props")
public class PropController {

	private final PropService propService;

	public PropController(PropService propService) {
		this.propService = propService;
	}

	@GetMapping
	public List<PropResponse> list() {
		return propService.findAll();
	}

	@GetMapping("/{id}")
	public PropResponse getById(@PathVariable Long id) {
		return propService.findById(id);
	}

	@PostMapping
	public ResponseEntity<PropResponse> create(@Valid @RequestBody CreatePropRequest request) {
		PropResponse created = propService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@PatchMapping("/{id}")
	public PropResponse update(@PathVariable Long id, @Valid @RequestBody UpdatePropRequest request) {
		return propService.update(id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		propService.deleteById(id);
		return ResponseEntity.noContent().build();
	}
}
