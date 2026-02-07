package com.akandiah.propmanager.features.prop.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.features.address.domain.Address;
import com.akandiah.propmanager.features.address.domain.AddressRepository;
import com.akandiah.propmanager.features.prop.api.dto.CreatePropRequest;
import com.akandiah.propmanager.features.prop.api.dto.CreatePropRequest.AddressInput;
import com.akandiah.propmanager.features.prop.api.dto.PropResponse;
import com.akandiah.propmanager.features.prop.api.dto.UpdatePropRequest;
import com.akandiah.propmanager.features.prop.domain.Prop;
import com.akandiah.propmanager.features.prop.domain.PropRepository;

@Service
public class PropService {

	private final PropRepository repository;
	private final AddressRepository addressRepository;

	public PropService(PropRepository repository, AddressRepository addressRepository) {
		this.repository = repository;
		this.addressRepository = addressRepository;
	}

	@Transactional(readOnly = true)
	public List<PropResponse> findAll() {
		return repository.findAll().stream()
				.map(PropResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public PropResponse findById(UUID id) {
		Prop prop = repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Prop", id));
		return PropResponse.from(prop);
	}

	@Transactional
	public PropResponse create(CreatePropRequest request) {
		Address address = mapToAddress(request.address());
		address = addressRepository.save(address);
		Prop prop = Prop.builder()
				.legalName(request.legalName())
				.address(address)
				.propertyType(request.propertyType())
				.parcelNumber(request.parcelNumber())
				.ownerId(request.ownerId())
				.totalArea(request.totalArea())
				.yearBuilt(request.yearBuilt())
				.isActive(request.isActive() != null ? request.isActive() : true)
				.build();
		prop = repository.save(prop);
		return PropResponse.from(prop);
	}

	private static Address mapToAddress(AddressInput in) {
		return Address.builder()
				.addressLine1(in.addressLine1())
				.addressLine2(in.addressLine2())
				.city(in.city())
				.stateProvinceRegion(in.stateProvinceRegion())
				.postalCode(in.postalCode())
				.countryCode(in.countryCode())
				.latitude(in.latitude())
				.longitude(in.longitude())
				.build();
	}

	@Transactional
	public PropResponse update(UUID id, UpdatePropRequest request) {
		Prop prop = repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Prop", id));
		if (request.legalName() != null)
			prop.setLegalName(request.legalName());
		if (request.address() != null) {
			Address address = mapToAddress(request.address());
			address = addressRepository.save(address);
			prop.setAddress(address);
		}
		if (request.propertyType() != null)
			prop.setPropertyType(request.propertyType());
		if (request.parcelNumber() != null)
			prop.setParcelNumber(request.parcelNumber());
		if (request.ownerId() != null)
			prop.setOwnerId(request.ownerId());
		if (request.totalArea() != null)
			prop.setTotalArea(request.totalArea());
		if (request.yearBuilt() != null)
			prop.setYearBuilt(request.yearBuilt());
		if (request.isActive() != null)
			prop.setIsActive(request.isActive());
		prop = repository.save(prop);
		return PropResponse.from(prop);
	}

	@Transactional
	public void deleteById(UUID id) {
		if (!repository.existsById(id))
			throw new ResourceNotFoundException("Prop", id);
		repository.deleteById(id);
	}
}
