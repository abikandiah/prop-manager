package com.akandiah.propmanager.features.unit.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.common.permission.AccessListUtil.ScopedAccessFilter;
import com.akandiah.propmanager.common.permission.ResourceType;
import com.akandiah.propmanager.common.util.DeleteGuardUtil;
import com.akandiah.propmanager.common.util.OptimisticLockingUtil;
import com.akandiah.propmanager.features.auth.domain.PermissionsChangedEvent;
import com.akandiah.propmanager.features.asset.domain.AssetRepository;
import com.akandiah.propmanager.features.lease.domain.LeaseRepository;
import com.akandiah.propmanager.features.membership.domain.PolicyAssignmentRepository;
import com.akandiah.propmanager.features.prop.domain.Prop;
import com.akandiah.propmanager.features.prop.domain.PropRepository;
import com.akandiah.propmanager.features.unit.api.dto.CreateUnitRequest;
import com.akandiah.propmanager.features.unit.api.dto.UnitResponse;
import com.akandiah.propmanager.features.unit.api.dto.UpdateUnitRequest;
import com.akandiah.propmanager.features.unit.domain.Unit;
import com.akandiah.propmanager.features.unit.domain.UnitRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UnitService {

	private final UnitRepository unitRepository;
	private final PropRepository propRepository;
	private final AssetRepository assetRepository;
	private final LeaseRepository leaseRepository;
	private final PolicyAssignmentRepository assignmentRepository;
	private final ApplicationEventPublisher eventPublisher;

	public List<UnitResponse> findAll() {
		return unitRepository.findAll().stream()
				.map(UnitResponse::from)
				.toList();
	}

	public List<UnitResponse> findAll(ScopedAccessFilter filter, UUID orgId, UUID propId) {
		if (filter.isEmpty()) return List.of();
		return unitRepository.findByAccessFilter(filter.orgIds(), filter.propIds(), filter.unitIds(), orgId, propId).stream()
				.map(UnitResponse::from)
				.toList();
	}

	public List<UnitResponse> findByPropId(UUID propId) {
		return unitRepository.findByProp_IdOrderByUnitNumberAsc(propId).stream()
				.map(UnitResponse::from)
				.toList();
	}

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
				.id(request.id())
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

		// Collect userIds from affected assignments before deleting them
		Set<UUID> affectedUserIds = new HashSet<>();
		assignmentRepository.findByResourceTypeAndResourceId(ResourceType.UNIT, id)
				.forEach(a -> {
					if (a.getMembership().getUser() != null) {
						affectedUserIds.add(a.getMembership().getUser().getId());
					}
				});

		assignmentRepository.deleteByResourceTypeAndResourceId(ResourceType.UNIT, id);
		unitRepository.deleteById(id);

		if (!affectedUserIds.isEmpty()) {
			eventPublisher.publishEvent(new PermissionsChangedEvent(affectedUserIds));
		}
	}
}
