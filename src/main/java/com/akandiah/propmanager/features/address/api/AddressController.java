package com.akandiah.propmanager.features.address.api;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.akandiah.propmanager.features.address.api.dto.AddressResponse;
import com.akandiah.propmanager.features.address.api.dto.CreateAddressRequest;
import com.akandiah.propmanager.features.address.service.AddressService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/addresses")
@Tag(name = "Addresses", description = "Address resource")
public class AddressController {

	private final AddressService addressService;

	public AddressController(AddressService addressService) {
		this.addressService = addressService;
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get address by ID")
	public AddressResponse getById(@PathVariable UUID id) {
		return addressService.findById(id);
	}

	@PostMapping
	@Operation(summary = "Create an address")
	public ResponseEntity<AddressResponse> create(@Valid @RequestBody CreateAddressRequest request) {
		AddressResponse created = addressService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}
}
