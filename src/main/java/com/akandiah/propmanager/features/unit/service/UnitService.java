package com.akandiah.propmanager.features.unit.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.common.util.DeleteGuardUtil;
import com.akandiah.propmanager.common.util.OptimisticLockingUtil;
import com.akandiah.propmanager.features.asset.domain.AssetRepository;
import com.akandiah.propmanager.features.lease.domain.LeaseRepository;
import com.akandiah.propmanager.features.prop.domain.Prop;
import com.akandiah.propmanager.features.prop.domain.PropRepository;
import com.akandiah.propmanager.features.unit.api.dto.CreateUnitRequest;
import com.akandiah.propmanager.features.unit.api.dto.UnitResponse;
import com.akandiah.propmanager.features.unit.api.dto.UpdateUnitRequest;
import com.akandiah.propmanager.features.unit.domain.Unit;
import com.akandiah.propmanager.features.unit.domain.UnitRepository;

@Service
public class UnitService {

	private final UnitRepository unitRepository;
	private final PropRepository propRepository;
	private final AssetRepository assetRepository;
	private final LeaseRepository leaseRepository;

	public UnitService(UnitRepository unitRepository, PropRepository propRepository,
			AssetRepository assetRepository, LeaseRepository leaseRepository) {
		this.unitRepository = unitRepository;
		this.propRepository = propRepository;
		this.assetRepository = assetRepository;
		this.leaseRepository = leaseRepository;
	}

	@Transactional(readOnly = true)
	public List<UnitResponse> findAll() {
		return unitRepository.findAll().stream()
				.map(UnitResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<UnitResponse> findByPropId(UUID propId) {
		return unitRepository.findByProp_IdOrderByUnitNumberAsc(propId).stream()
				.map(UnitResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public UnitResponse findById(UUID id) {
		Unit unit = unitRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Unit", id));
		return UnitResponse.from(unit);
	}

	@Transactional
	public UnitResponse create(CreateUnitRequest request) {
		Prop prop = propRepository.findById(request.propertyId())
				.orElseThrow(() -> new ResourceNotFoundException("Prop", request.propertyId()));
		Unit unit = Unit.builder()
				.prop(prop)
				.unitNumber(request.unitNumber())
				.status(request.status())
				.unitType(request.unitType())
				.description(request.description())
				.rentAmount(request.rentAmount())
				.securityDeposit(request.securityDeposit())
				.bedrooms(request.bedrooms())
				.bathrooms(request.bathrooms())
				.squareFootage(request.squareFootage())
				.balcony(request.balcony())
				.laundryInUnit(request.laundryInUnit())
				.hardwoodFloors(request.hardwoodFloors())
				.build();
		unit = unitRepository.save(unit);
		return UnitResponse.from(unit);
	}

	@Transactional
	public UnitResponse update(UUID id, UpdateUnitRequest request) {
		Unit unit = unitRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Unit", id));
		OptimisticLockingUtil.requireVersionMatch("Unit", id, unit.getVersion(), request.version());
		if (request.propertyId() != null) {
			Prop prop = propRepository.findById(request.propertyId())
					.orElseThrow(() -> new ResourceNotFoundException("Prop", request.propertyId()));
			unit.setProp(prop);
		}
		if (request.unitNumber() != null) {
			unit.setUnitNumber(request.unitNumber());
		}
		if (request.status() != null) {
			unit.setStatus(request.status());
		}
		if (request.unitType() != null) {
			unit.setUnitType(request.unitType());
		}
		if (request.description() != null) {
			unit.setDescription(request.description());
		}
		if (request.rentAmount() != null) {
			unit.setRentAmount(request.rentAmount());
		}
		if (request.securityDeposit() != null) {
			unit.setSecurityDeposit(request.securityDeposit());
		}
		if (request.bedrooms() != null) {
			unit.setBedrooms(request.bedrooms());
		}
		if (request.bathrooms() != null) {
			unit.setBathrooms(request.bathrooms());
		}
		if (request.squareFootage() != null) {
			unit.setSquareFootage(request.squareFootage());
		}
		if (request.balcony() != null) {
			unit.setBalcony(request.balcony());
		}
		if (request.laundryInUnit() != null) {
			unit.setLaundryInUnit(request.laundryInUnit());
		}
		if (request.hardwoodFloors() != null) {
			unit.setHardwoodFloors(request.hardwoodFloors());
		}
		unit = unitRepository.save(unit);
		return UnitResponse.from(unit);
	}

	@Transactional
	public void deleteById(UUID id) {
		if (!unitRepository.existsById(id)) {
			throw new ResourceNotFoundException("Unit", id);
		}

		// Guard against orphaning child records
		DeleteGuardUtil.requireNoChildren("Unit", id, assetRepository.countByUnit_Id(id), "asset(s)", "Delete those first.");
		DeleteGuardUtil.requireNoChildren("Unit", id, leaseRepository.countByUnit_Id(id), "lease(s)", "Delete those first.");

		unitRepository.deleteById(id);
	}
}
