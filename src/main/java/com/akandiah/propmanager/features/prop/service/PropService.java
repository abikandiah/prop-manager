package com.akandiah.propmanager.features.prop.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.features.address.domain.Address;
import com.akandiah.propmanager.features.address.domain.AddressRepository;
import com.akandiah.propmanager.features.asset.domain.AssetRepository;
import com.akandiah.propmanager.features.lease.domain.LeaseRepository;
import com.akandiah.propmanager.features.prop.api.dto.CreatePropRequest;
import com.akandiah.propmanager.features.prop.api.dto.CreatePropRequest.AddressInput;
import com.akandiah.propmanager.features.prop.api.dto.PropResponse;
import com.akandiah.propmanager.features.prop.api.dto.UpdatePropRequest;
import com.akandiah.propmanager.features.prop.domain.Prop;
import com.akandiah.propmanager.features.prop.domain.PropRepository;
import com.akandiah.propmanager.features.unit.domain.UnitRepository;

import jakarta.persistence.OptimisticLockException;

@Service
public class PropService {

	private final PropRepository repository;
	private final AddressRepository addressRepository;
	private final UnitRepository unitRepository;
	private final AssetRepository assetRepository;
	private final LeaseRepository leaseRepository;

	public PropService(PropRepository repository, AddressRepository addressRepository,
			UnitRepository unitRepository, AssetRepository assetRepository,
			LeaseRepository leaseRepository) {
		this.repository = repository;
		this.addressRepository = addressRepository;
		this.unitRepository = unitRepository;
		this.assetRepository = assetRepository;
		this.leaseRepository = leaseRepository;
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
				.description(request.description())
				.parcelNumber(request.parcelNumber())
				.ownerId(request.ownerId())
				.totalArea(request.totalArea())
				.yearBuilt(request.yearBuilt())
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
		requireVersionMatch(prop, request.version());
		if (request.legalName() != null)
			prop.setLegalName(request.legalName());
		if (request.address() != null) {
			Address address = mapToAddress(request.address());
			address = addressRepository.save(address);
			prop.setAddress(address);
		}
		if (request.propertyType() != null)
			prop.setPropertyType(request.propertyType());
		if (request.description() != null)
			prop.setDescription(request.description());
		if (request.parcelNumber() != null)
			prop.setParcelNumber(request.parcelNumber());
		if (request.ownerId() != null)
			prop.setOwnerId(request.ownerId());
		if (request.totalArea() != null)
			prop.setTotalArea(request.totalArea());
		if (request.yearBuilt() != null)
			prop.setYearBuilt(request.yearBuilt());
		prop = repository.save(prop);
		return PropResponse.from(prop);
	}

	@Transactional
	public void deleteById(UUID id) {
		if (!repository.existsById(id))
			throw new ResourceNotFoundException("Prop", id);

		// Guard against orphaning child records
		long unitCount = unitRepository.countByProp_Id(id);
		if (unitCount > 0)
			throw new IllegalStateException(
					"Cannot delete Prop " + id + ": it has " + unitCount + " unit(s). Delete those first.");

		long assetCount = assetRepository.countByProp_Id(id);
		if (assetCount > 0)
			throw new IllegalStateException(
					"Cannot delete Prop " + id + ": it has " + assetCount + " asset(s). Delete those first.");

		long leaseCount = leaseRepository.countByProperty_Id(id);
		if (leaseCount > 0)
			throw new IllegalStateException(
					"Cannot delete Prop " + id + ": it has " + leaseCount + " lease(s). Delete those first.");

		repository.deleteById(id);
	}

	private void requireVersionMatch(Prop prop, Integer clientVersion) {
		if (!prop.getVersion().equals(clientVersion)) {
			throw new OptimisticLockException(
					"Prop " + prop.getId() + " has been modified by another user. "
							+ "Expected version " + clientVersion
							+ " but current version is " + prop.getVersion());
		}
	}
}
