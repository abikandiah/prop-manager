package com.akandiah.propmanager.features.prop.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.common.permission.AccessListUtil.PropAccessFilter;
import com.akandiah.propmanager.common.permission.ResourceType;
import com.akandiah.propmanager.common.util.DeleteGuardUtil;
import com.akandiah.propmanager.common.util.OptimisticLockingUtil;
import com.akandiah.propmanager.features.auth.domain.PermissionsChangedEvent;
import com.akandiah.propmanager.features.asset.domain.AssetRepository;
import com.akandiah.propmanager.features.lease.domain.LeaseRepository;
import com.akandiah.propmanager.features.membership.domain.PolicyAssignmentRepository;
import com.akandiah.propmanager.features.organization.domain.Organization;
import com.akandiah.propmanager.features.organization.domain.OrganizationRepository;
import com.akandiah.propmanager.features.prop.api.dto.CreatePropRequest;
import com.akandiah.propmanager.features.prop.api.dto.CreatePropRequest.AddressInput;
import com.akandiah.propmanager.features.prop.api.dto.PropResponse;
import com.akandiah.propmanager.features.prop.api.dto.UpdatePropRequest;
import com.akandiah.propmanager.features.prop.domain.Address;
import com.akandiah.propmanager.features.prop.domain.AddressRepository;
import com.akandiah.propmanager.features.prop.domain.Prop;
import com.akandiah.propmanager.features.prop.domain.PropRepository;
import com.akandiah.propmanager.features.unit.domain.UnitRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PropService {

	private final PropRepository repository;
	private final AddressRepository addressRepository;
	private final OrganizationRepository organizationRepository;
	private final UnitRepository unitRepository;
	private final AssetRepository assetRepository;
	private final LeaseRepository leaseRepository;
	private final PolicyAssignmentRepository assignmentRepository;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional(readOnly = true)
	public List<PropResponse> findAll(PropAccessFilter filter) {
		if (filter.isEmpty()) return List.of();
		return repository.findByOrganizationIdInOrIdIn(filter.orgIds(), filter.propIds()).stream()
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
	public PropResponse create(CreatePropRequest request, UUID orgId) {
		Address address = mapToAddress(request.address());
		address = addressRepository.save(address);
		Organization organization = organizationRepository.findById(orgId)
				.orElseThrow(() -> new ResourceNotFoundException("Organization", orgId));
		Prop prop = Prop.builder()
				.id(request.id())
				.legalName(request.legalName())
				.address(address)
				.propertyType(request.propertyType())
				.description(request.description())
				.parcelNumber(request.parcelNumber())
				.organization(organization)
				.ownerId(request.ownerId())
				.totalArea(request.totalArea())
				.yearBuilt(request.yearBuilt())
				.build();
		prop = repository.save(prop);
		if (prop.getOwnerId() != null) {
			eventPublisher.publishEvent(new PermissionsChangedEvent(Set.of(prop.getOwnerId())));
		}
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

	private void updateAddress(Address address, AddressInput in) {
		address.setAddressLine1(in.addressLine1());
		address.setAddressLine2(in.addressLine2());
		address.setCity(in.city());
		address.setStateProvinceRegion(in.stateProvinceRegion());
		address.setPostalCode(in.postalCode());
		address.setCountryCode(in.countryCode());
		address.setLatitude(in.latitude());
		address.setLongitude(in.longitude());
	}

	@Transactional
	public PropResponse update(UUID id, UpdatePropRequest request) {
		Prop prop = repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Prop", id));
		OptimisticLockingUtil.requireVersionMatch("Prop", id, prop.getVersion(), request.version());

		if (request.legalName() != null) {
			prop.setLegalName(request.legalName());
		}
		if (request.address() != null) {
			updateAddress(prop.getAddress(), request.address());
		}
		if (request.propertyType() != null) {
			prop.setPropertyType(request.propertyType());
		}
		if (request.description() != null) {
			prop.setDescription(request.description());
		}
		if (request.parcelNumber() != null) {
			prop.setParcelNumber(request.parcelNumber());
		}
		if (request.organizationId() != null) {
			Organization org = organizationRepository.findById(request.organizationId())
					.orElseThrow(() -> new ResourceNotFoundException("Organization", request.organizationId()));
			prop.setOrganization(org);
		}
		Set<UUID> affectedOwners = new HashSet<>();
		if (request.ownerId() != null && !request.ownerId().equals(prop.getOwnerId())) {
			if (prop.getOwnerId() != null) {
				affectedOwners.add(prop.getOwnerId());
			}
			affectedOwners.add(request.ownerId());
			prop.setOwnerId(request.ownerId());
		}
		if (request.totalArea() != null) {
			prop.setTotalArea(request.totalArea());
		}
		if (request.yearBuilt() != null) {
			prop.setYearBuilt(request.yearBuilt());
		}
		prop = repository.save(prop);
		if (!affectedOwners.isEmpty()) {
			eventPublisher.publishEvent(new PermissionsChangedEvent(affectedOwners));
		}
		return PropResponse.from(prop);
	}

	@Transactional
	public void deleteById(UUID id) {
		Prop prop = repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Prop", id));

		// Guard against orphaning child records
		DeleteGuardUtil.requireNoChildren("Prop", id, unitRepository.countByProp_Id(id), "unit(s)", "Delete those first.");
		DeleteGuardUtil.requireNoChildren("Prop", id, assetRepository.countByProp_Id(id), "asset(s)", "Delete those first.");
		DeleteGuardUtil.requireNoChildren("Prop", id, leaseRepository.countByProperty_Id(id), "lease(s)", "Delete those first.");

		// Collect userIds from affected assignments before deleting them
		Set<UUID> affectedUserIds = new HashSet<>();
		assignmentRepository.findByResourceTypeAndResourceId(ResourceType.PROPERTY, id)
				.forEach(a -> {
					if (a.getMembership().getUser() != null) {
						affectedUserIds.add(a.getMembership().getUser().getId());
					}
				});
		assignmentRepository.deleteByResourceTypeAndResourceId(ResourceType.PROPERTY, id);
		UUID ownerId = prop.getOwnerId();
		if (ownerId != null) {
			affectedUserIds.add(ownerId);
		}
		Address address = prop.getAddress();
		repository.deleteById(id);
		addressRepository.delete(address);
		if (!affectedUserIds.isEmpty()) {
			eventPublisher.publishEvent(new PermissionsChangedEvent(affectedUserIds));
		}
	}
}
