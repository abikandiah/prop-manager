package com.akandiah.propmanager.features.address.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.features.address.api.dto.AddressResponse;
import com.akandiah.propmanager.features.address.api.dto.CreateAddressRequest;
import com.akandiah.propmanager.features.address.domain.Address;
import com.akandiah.propmanager.features.address.domain.AddressRepository;

@Service
public class AddressService {

	private final AddressRepository repository;

	public AddressService(AddressRepository repository) {
		this.repository = repository;
	}

	@Transactional(readOnly = true)
	public AddressResponse findById(UUID id) {
		Address address = repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Address", id));
		return AddressResponse.from(address);
	}

	@Transactional
	public AddressResponse create(CreateAddressRequest request) {
		Address address = Address.builder()
				.addressLine1(request.addressLine1())
				.addressLine2(request.addressLine2())
				.city(request.city())
				.stateProvinceRegion(request.stateProvinceRegion())
				.postalCode(request.postalCode())
				.countryCode(request.countryCode())
				.latitude(request.latitude())
				.longitude(request.longitude())
				.build();
		address = repository.save(address);
		return AddressResponse.from(address);
	}
}
