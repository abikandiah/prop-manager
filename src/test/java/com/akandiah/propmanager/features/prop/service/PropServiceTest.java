package com.akandiah.propmanager.features.prop.service;

import static com.akandiah.propmanager.TestDataFactory.address;
import static com.akandiah.propmanager.TestDataFactory.prop;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.akandiah.propmanager.common.exception.HasChildrenException;
import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.common.permission.AccessListUtil.PropAccessFilter;
import com.akandiah.propmanager.features.organization.domain.Organization;
import com.akandiah.propmanager.features.asset.domain.AssetRepository;
import com.akandiah.propmanager.features.lease.domain.LeaseRepository;
import com.akandiah.propmanager.features.membership.domain.MemberScopeRepository;
import com.akandiah.propmanager.features.organization.domain.OrganizationRepository;
import com.akandiah.propmanager.features.prop.api.dto.CreatePropRequest;
import com.akandiah.propmanager.features.prop.api.dto.PropResponse;
import com.akandiah.propmanager.features.prop.api.dto.UpdatePropRequest;
import com.akandiah.propmanager.features.prop.domain.Address;
import com.akandiah.propmanager.features.prop.domain.AddressRepository;
import com.akandiah.propmanager.features.prop.domain.Prop;
import com.akandiah.propmanager.features.prop.domain.PropRepository;
import com.akandiah.propmanager.features.prop.domain.PropertyType;
import com.akandiah.propmanager.features.unit.domain.UnitRepository;

import jakarta.persistence.OptimisticLockException;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Unit tests for {@link PropService}.
 * Tests property management business logic with mocked repositories.
 */
@ExtendWith(MockitoExtension.class)
class PropServiceTest {

	@Mock
	private PropRepository propRepository;

	@Mock
	private AddressRepository addressRepository;

	@Mock
	private UnitRepository unitRepository;

	@Mock
	private AssetRepository assetRepository;

	@Mock
	private LeaseRepository leaseRepository;

	@Mock
	private OrganizationRepository organizationRepository;

	@Mock
	private MemberScopeRepository memberScopeRepository;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	private PropService propService;

	@BeforeEach
	void setUp() {
		propService = new PropService(propRepository, addressRepository,
				organizationRepository, unitRepository, assetRepository, leaseRepository,
				memberScopeRepository, eventPublisher);
	}

	// ═══════════════════════════════════════════════════════════════════════
	// FindAll
	// ═══════════════════════════════════════════════════════════════════════

	@Test
	void shouldReturnAllProps() {
		UUID orgId = UUID.randomUUID();
		Prop prop1 = prop().id(UUID.randomUUID()).legalName("Prop 1").build();
		Prop prop2 = prop().id(UUID.randomUUID()).legalName("Prop 2").build();
		PropAccessFilter filter = new PropAccessFilter(Set.of(orgId), Set.of());

		when(propRepository.findByOrganizationIdInOrIdIn(Set.of(orgId), Set.of()))
				.thenReturn(Arrays.asList(prop1, prop2));

		List<PropResponse> responses = propService.findAll(filter);

		assertThat(responses).hasSize(2);
		assertThat(responses).extracting("legalName").containsExactly("Prop 1", "Prop 2");
	}

	@Test
	void shouldReturnEmptyListWhenNoProps() {
		PropAccessFilter emptyFilter = new PropAccessFilter(Set.of(), Set.of());

		List<PropResponse> responses = propService.findAll(emptyFilter);

		assertThat(responses).isEmpty();
	}

	// ═══════════════════════════════════════════════════════════════════════
	// FindById
	// ═══════════════════════════════════════════════════════════════════════

	@Test
	void shouldReturnPropById() {
		UUID propId = UUID.randomUUID();
		Prop prop = prop()
				.id(propId)
				.legalName("Test Property")
				.description("Test description")
				.build();

		when(propRepository.findById(propId)).thenReturn(Optional.of(prop));

		PropResponse response = propService.findById(propId);

		assertThat(response.id()).isEqualTo(propId);
		assertThat(response.legalName()).isEqualTo("Test Property");
		assertThat(response.description()).isEqualTo("Test description");
	}

	@Test
	void shouldThrowResourceNotFoundExceptionWhenPropNotFound() {
		UUID propId = UUID.randomUUID();

		when(propRepository.findById(propId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> propService.findById(propId))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Prop")
				.hasMessageContaining(propId.toString())
				.hasMessageContaining("not found");
	}

	// ═══════════════════════════════════════════════════════════════════════
	// Create
	// ═══════════════════════════════════════════════════════════════════════

	@Test
	void shouldCreatePropWithAddress() {
		UUID orgId = UUID.randomUUID();
		Organization org = Organization.builder().id(orgId).build();
		Address savedAddress = address().id(UUID.randomUUID()).build();
		CreatePropRequest request = prop().organization(org).buildCreateRequest();

		when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
		when(addressRepository.save(any(Address.class))).thenReturn(savedAddress);
		when(propRepository.save(any(Prop.class))).thenAnswer(invocation -> {
			Prop prop = invocation.getArgument(0);
			return prop().id(UUID.randomUUID())
					.legalName(prop.getLegalName())
					.address(prop.getAddress())
					.propertyType(prop.getPropertyType())
					.build();
		});

		PropResponse response = propService.create(request);

		assertThat(response.legalName()).isEqualTo(request.legalName());
		assertThat(response.propertyType()).isEqualTo(request.propertyType());
		assertThat(response.addressId()).isEqualTo(savedAddress.getId());

		verify(addressRepository).save(any(Address.class));
		verify(propRepository).save(any(Prop.class));
	}

	@Test
	void shouldMapAddressFieldsCorrectly() {
		UUID orgId = UUID.randomUUID();
		Organization org = Organization.builder().id(orgId).build();
		CreatePropRequest request = prop()
				.organization(org)
				.address(address()
						.addressLine1("456 Oak Avenue")
						.city("Vancouver")
						.postalCode("V6B 1A1")
						.build())
				.buildCreateRequest();

		ArgumentCaptor<Address> addressCaptor = ArgumentCaptor.forClass(Address.class);

		when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
		when(addressRepository.save(addressCaptor.capture())).thenAnswer(invocation -> {
			Address addr = invocation.getArgument(0);
			return address()
					.id(UUID.randomUUID())
					.addressLine1(addr.getAddressLine1())
					.city(addr.getCity())
					.postalCode(addr.getPostalCode())
					.build();
		});
		when(propRepository.save(any(Prop.class))).thenAnswer(invocation -> {
			Prop p = invocation.getArgument(0);
			return prop().id(UUID.randomUUID()).address(p.getAddress()).build();
		});

		propService.create(request);

		Address capturedAddress = addressCaptor.getValue();
		assertThat(capturedAddress.getAddressLine1()).isEqualTo("456 Oak Avenue");
		assertThat(capturedAddress.getCity()).isEqualTo("Vancouver");
		assertThat(capturedAddress.getPostalCode()).isEqualTo("V6B 1A1");
	}

	// ═══════════════════════════════════════════════════════════════════════
	// Update
	// ═══════════════════════════════════════════════════════════════════════

	@Test
	void shouldUpdatePropFields() {
		UUID propId = UUID.randomUUID();
		Prop existingProp = prop()
				.id(propId)
				.legalName("Old Name")
				.description("Old description")
				.propertyType(PropertyType.APARTMENT_BUILDING)
				.version(0)
				.build();

		UpdatePropRequest request = prop()
				.legalName("New Name")
				.description("New description")
				.propertyType(PropertyType.CONDOMINIUM)
				.version(0)
				.buildUpdateRequest();

		when(propRepository.findById(propId)).thenReturn(Optional.of(existingProp));
		when(propRepository.save(any(Prop.class))).thenAnswer(invocation -> invocation.getArgument(0));

		PropResponse response = propService.update(propId, request);

		assertThat(response.legalName()).isEqualTo("New Name");
		assertThat(response.description()).isEqualTo("New description");
		assertThat(response.propertyType()).isEqualTo(PropertyType.CONDOMINIUM);
		verify(propRepository).save(existingProp);
	}

	@Test
	void shouldUpdateOnlyProvidedFields() {
		UUID propId = UUID.randomUUID();
		Prop existingProp = prop()
				.id(propId)
				.legalName("Original Name")
				.description("Original description")
				.totalArea(5000)
				.version(0)
				.build();

		// Only update legal name
		UpdatePropRequest request = new UpdatePropRequest(
				"Updated Name",
				null, // address not updated
				null, // propertyType not updated
				null, // description not updated
				null, // parcelNumber not updated
				null, // organizationId not updated
				null, // ownerId not updated
				null, // totalArea not updated
				null, // yearBuilt not updated
				0);  // version

		when(propRepository.findById(propId)).thenReturn(Optional.of(existingProp));
		when(propRepository.save(any(Prop.class))).thenAnswer(invocation -> invocation.getArgument(0));

		PropResponse response = propService.update(propId, request);

		assertThat(response.legalName()).isEqualTo("Updated Name");
		assertThat(response.description()).isEqualTo("Original description");
		assertThat(response.totalArea()).isEqualTo(5000);
	}

	@Test
	void shouldUpdateAddressFields() {
		UUID propId = UUID.randomUUID();
		Address existingAddress = address()
				.id(UUID.randomUUID())
				.addressLine1("Old Street")
				.city("Old City")
				.build();

		Prop existingProp = prop()
				.id(propId)
				.address(existingAddress)
				.version(0)
				.build();

		UpdatePropRequest request = prop()
				.address(address()
						.addressLine1("New Street")
						.city("New City")
						.build())
				.version(0)
				.buildUpdateRequest();

		when(propRepository.findById(propId)).thenReturn(Optional.of(existingProp));
		when(propRepository.save(any(Prop.class))).thenAnswer(invocation -> invocation.getArgument(0));

		propService.update(propId, request);

		assertThat(existingAddress.getAddressLine1()).isEqualTo("New Street");
		assertThat(existingAddress.getCity()).isEqualTo("New City");
	}

	@Test
	void shouldThrowOptimisticLockExceptionOnVersionMismatch() {
		UUID propId = UUID.randomUUID();
		Prop existingProp = prop()
				.id(propId)
				.version(5)
				.build();

		UpdatePropRequest request = prop()
				.version(3) // Stale version
				.buildUpdateRequest();

		when(propRepository.findById(propId)).thenReturn(Optional.of(existingProp));

		assertThatThrownBy(() -> propService.update(propId, request))
				.isInstanceOf(OptimisticLockException.class)
				.hasMessageContaining("Prop")
				.hasMessageContaining(propId.toString())
				.hasMessageContaining("Expected version 3")
				.hasMessageContaining("current version is 5")
				.hasMessageContaining("modified by another user");

		verify(propRepository, never()).save(any(Prop.class));
	}

	@Test
	void shouldThrowResourceNotFoundExceptionWhenUpdatingNonExistentProp() {
		UUID propId = UUID.randomUUID();
		UpdatePropRequest request = prop().buildUpdateRequest();

		when(propRepository.findById(propId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> propService.update(propId, request))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Prop")
				.hasMessageContaining(propId.toString());
	}

	// ═══════════════════════════════════════════════════════════════════════
	// Delete
	// ═══════════════════════════════════════════════════════════════════════

	@Test
	void shouldDeletePropAndAddressWhenNoChildren() {
		UUID propId = UUID.randomUUID();
		Address address = address().id(UUID.randomUUID()).build();
		Prop prop = prop().id(propId).address(address).build();

		when(propRepository.findById(propId)).thenReturn(Optional.of(prop));
		when(unitRepository.countByProp_Id(propId)).thenReturn(0L);
		when(assetRepository.countByProp_Id(propId)).thenReturn(0L);
		when(leaseRepository.countByProperty_Id(propId)).thenReturn(0L);

		propService.deleteById(propId);

		verify(propRepository).deleteById(propId);
		verify(addressRepository).delete(address);
	}

	@Test
	void shouldThrowHasChildrenExceptionWhenDeletingPropWithUnits() {
		UUID propId = UUID.randomUUID();
		Prop prop = prop().id(propId).build();

		when(propRepository.findById(propId)).thenReturn(Optional.of(prop));
		when(unitRepository.countByProp_Id(propId)).thenReturn(3L);

		assertThatThrownBy(() -> propService.deleteById(propId))
				.isInstanceOf(HasChildrenException.class)
				.hasMessageContaining("Cannot delete Prop")
				.hasMessageContaining(propId.toString())
				.hasMessageContaining("it has 3 unit(s)")
				.hasMessageContaining("Delete those first.");

		verify(propRepository, never()).deleteById(any());
		verify(addressRepository, never()).delete(any());
	}

	@Test
	void shouldThrowHasChildrenExceptionWhenDeletingPropWithAssets() {
		UUID propId = UUID.randomUUID();
		Prop prop = prop().id(propId).build();

		when(propRepository.findById(propId)).thenReturn(Optional.of(prop));
		when(unitRepository.countByProp_Id(propId)).thenReturn(0L);
		when(assetRepository.countByProp_Id(propId)).thenReturn(5L);

		assertThatThrownBy(() -> propService.deleteById(propId))
				.isInstanceOf(HasChildrenException.class)
				.hasMessageContaining("it has 5 asset(s)");

		verify(propRepository, never()).deleteById(any());
	}

	@Test
	void shouldThrowHasChildrenExceptionWhenDeletingPropWithLeases() {
		UUID propId = UUID.randomUUID();
		Prop prop = prop().id(propId).build();

		when(propRepository.findById(propId)).thenReturn(Optional.of(prop));
		when(unitRepository.countByProp_Id(propId)).thenReturn(0L);
		when(assetRepository.countByProp_Id(propId)).thenReturn(0L);
		when(leaseRepository.countByProperty_Id(propId)).thenReturn(2L);

		assertThatThrownBy(() -> propService.deleteById(propId))
				.isInstanceOf(HasChildrenException.class)
				.hasMessageContaining("it has 2 lease(s)");

		verify(propRepository, never()).deleteById(any());
	}

	@Test
	void shouldThrowResourceNotFoundExceptionWhenDeletingNonExistentProp() {
		UUID propId = UUID.randomUUID();

		when(propRepository.findById(propId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> propService.deleteById(propId))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Prop")
				.hasMessageContaining(propId.toString());

		verify(propRepository, never()).deleteById(any());
	}

	@Test
	void shouldCheckAllChildrenBeforeDeleting() {
		UUID propId = UUID.randomUUID();
		Prop prop = prop().id(propId).build();

		when(propRepository.findById(propId)).thenReturn(Optional.of(prop));

		// Verify all child checks are performed
		propService.deleteById(propId);

		verify(unitRepository).countByProp_Id(propId);
		verify(assetRepository).countByProp_Id(propId);
		verify(leaseRepository).countByProperty_Id(propId);
	}
}
