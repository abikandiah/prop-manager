package com.akandiah.propmanager.features.lease.service;

import static com.akandiah.propmanager.TestDataFactory.lease;
import static com.akandiah.propmanager.TestDataFactory.leaseTemplate;
import static com.akandiah.propmanager.TestDataFactory.prop;
import static com.akandiah.propmanager.TestDataFactory.unit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.akandiah.propmanager.common.exception.HasChildrenException;
import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.features.lease.api.dto.CreateLeaseRequest;
import com.akandiah.propmanager.features.lease.api.dto.LeaseResponse;
import com.akandiah.propmanager.features.lease.api.dto.UpdateLeaseRequest;
import org.springframework.context.ApplicationEventPublisher;

import com.akandiah.propmanager.features.lease.domain.LateFeeType;
import com.akandiah.propmanager.features.lease.domain.Lease;
import com.akandiah.propmanager.features.lease.domain.LeaseRepository;
import com.akandiah.propmanager.features.lease.domain.LeaseStatus;
import com.akandiah.propmanager.features.lease.domain.LeaseTemplate;
import com.akandiah.propmanager.features.lease.domain.LeaseTenantRepository;
import com.akandiah.propmanager.features.prop.domain.Prop;
import com.akandiah.propmanager.features.prop.domain.PropRepository;
import com.akandiah.propmanager.features.unit.domain.Unit;
import com.akandiah.propmanager.features.unit.domain.UnitRepository;
import com.akandiah.propmanager.security.JwtUserResolver;

import jakarta.persistence.OptimisticLockException;

/**
 * Unit tests for {@link LeaseService}.
 * Tests lease creation, updates, status transitions, and business logic.
 */
@ExtendWith(MockitoExtension.class)
class LeaseServiceTest {

	@Mock
	private LeaseRepository leaseRepository;

	@Mock
	private LeaseTemplateService templateService;

	@Mock
	private UnitRepository unitRepository;

	@Mock
	private PropRepository propRepository;

	@Mock
	private LeaseTenantRepository leaseTenantRepository;

	@Mock
	private LeaseStateMachine stateMachine;

	@Mock
	private LeaseTemplateRenderer renderer;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@Mock
	private JwtUserResolver jwtUserResolver;

	private LeaseService leaseService;

	@BeforeEach
	void setUp() {
		leaseService = new LeaseService(leaseRepository, templateService,
				unitRepository, propRepository, leaseTenantRepository,
				stateMachine, renderer, eventPublisher, jwtUserResolver);
	}

	// ═══════════════════════════════════════════════════════════════════════
	// FindAll
	// ═══════════════════════════════════════════════════════════════════════

	@Test
	void shouldReturnAllLeases() {
		Lease lease1 = lease().id(UUID.randomUUID()).build();
		Lease lease2 = lease().id(UUID.randomUUID()).build();

		when(leaseRepository.findAll()).thenReturn(Arrays.asList(lease1, lease2));

		List<LeaseResponse> responses = leaseService.findAll();

		assertThat(responses).hasSize(2);
	}

	// ═══════════════════════════════════════════════════════════════════════
	// FindByUnitId
	// ═══════════════════════════════════════════════════════════════════════

	@Test
	void shouldReturnLeasesByUnitId() {
		UUID unitId = UUID.randomUUID();
		Lease lease1 = lease().id(UUID.randomUUID()).build();
		Lease lease2 = lease().id(UUID.randomUUID()).build();

		when(leaseRepository.findByUnit_IdOrderByStartDateDesc(unitId))
				.thenReturn(Arrays.asList(lease1, lease2));

		List<LeaseResponse> responses = leaseService.findByUnitId(unitId);

		assertThat(responses).hasSize(2);
		verify(leaseRepository).findByUnit_IdOrderByStartDateDesc(unitId);
	}

	// ═══════════════════════════════════════════════════════════════════════
	// FindByPropertyId
	// ═══════════════════════════════════════════════════════════════════════

	@Test
	void shouldReturnLeasesByPropertyId() {
		UUID propertyId = UUID.randomUUID();
		Lease lease1 = lease().id(UUID.randomUUID()).build();
		Lease lease2 = lease().id(UUID.randomUUID()).build();

		when(leaseRepository.findByProperty_IdOrderByStartDateDesc(propertyId))
				.thenReturn(Arrays.asList(lease1, lease2));

		List<LeaseResponse> responses = leaseService.findByPropertyId(propertyId);

		assertThat(responses).hasSize(2);
		verify(leaseRepository).findByProperty_IdOrderByStartDateDesc(propertyId);
	}

	// ═══════════════════════════════════════════════════════════════════════
	// FindById
	// ═══════════════════════════════════════════════════════════════════════

	@Test
	void shouldReturnLeaseById() {
		UUID leaseId = UUID.randomUUID();
		Lease lease = lease()
				.id(leaseId)
				.rentAmount(new BigDecimal("2500.00"))
				.build();

		when(leaseRepository.findById(leaseId)).thenReturn(Optional.of(lease));

		LeaseResponse response = leaseService.findById(leaseId);

		assertThat(response.id()).isEqualTo(leaseId);
		assertThat(response.rentAmount()).isEqualByComparingTo("2500.00");
	}

	@Test
	void shouldThrowResourceNotFoundExceptionWhenLeaseNotFound() {
		UUID leaseId = UUID.randomUUID();

		when(leaseRepository.findById(leaseId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> leaseService.findById(leaseId))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Lease")
				.hasMessageContaining(leaseId.toString());
	}

	// ═══════════════════════════════════════════════════════════════════════
	// Create - Lease Stamping from Template
	// ═══════════════════════════════════════════════════════════════════════

	@Test
	void shouldCreateLeaseFromTemplate() {
		UUID templateId = UUID.randomUUID();
		UUID unitId = UUID.randomUUID();
		UUID propertyId = UUID.randomUUID();

		LeaseTemplate template = leaseTemplate()
				.id(templateId)
				.name("Standard Lease")
				.versionTag("v1.0")
				.defaultLateFeeType(LateFeeType.FLAT_FEE)
				.defaultLateFeeAmount(new BigDecimal("50.00"))
				.defaultNoticePeriodDays(60)
				.build();

		Unit leaseUnit = unit().id(unitId).unitNumber("101").build();
		Prop property = prop().id(propertyId).legalName("Test Building").build();

		CreateLeaseRequest request = new CreateLeaseRequest(
					null,
				templateId,
				unitId,
				propertyId,
				LocalDate.of(2026, 4, 1),
				LocalDate.of(2027, 3, 31),
				new BigDecimal("2000.00"),
				1,
				new BigDecimal("2000.00"),
				null, // Use template default
				null, // Use template default
				null, // Use template default
				null,
				null);

		when(templateService.getEntity(templateId)).thenReturn(template);
		when(unitRepository.findById(unitId)).thenReturn(Optional.of(leaseUnit));
		when(propRepository.findById(propertyId)).thenReturn(Optional.of(property));
		when(leaseRepository.save(any(Lease.class))).thenAnswer(invocation -> {
			Lease l = invocation.getArgument(0);
			return lease()
					.id(UUID.randomUUID())
					.leaseTemplate(l.getLeaseTemplate())
					.unit(l.getUnit())
					.property(l.getProperty())
					.status(l.getStatus())
					.rentAmount(l.getRentAmount())
					.lateFeeType(l.getLateFeeType())
					.lateFeeAmount(l.getLateFeeAmount())
					.noticePeriodDays(l.getNoticePeriodDays())
					.executedContentMarkdown(l.getExecutedContentMarkdown())
					.build();
		});

		LeaseResponse response = leaseService.create(request);

		assertThat(response.status()).isEqualTo(LeaseStatus.DRAFT);
		assertThat(response.rentAmount()).isEqualByComparingTo("2000.00");
		assertThat(response.lateFeeType()).isEqualTo(LateFeeType.FLAT_FEE);
		assertThat(response.lateFeeAmount()).isEqualByComparingTo("50.00");
		assertThat(response.noticePeriodDays()).isEqualTo(60);
		assertThat(response.executedContentMarkdown()).isNull(); // Stamped on activate, not create

		verify(templateService).getEntity(templateId);
		verify(unitRepository).findById(unitId);
		verify(propRepository).findById(propertyId);
		verify(leaseRepository).save(any(Lease.class));
	}

	@Test
	void shouldOverrideTemplateDefaultsWhenProvided() {
		UUID templateId = UUID.randomUUID();
		UUID unitId = UUID.randomUUID();
		UUID propertyId = UUID.randomUUID();

		LeaseTemplate template = leaseTemplate()
				.id(templateId)
				.defaultLateFeeType(LateFeeType.FLAT_FEE)
				.defaultLateFeeAmount(new BigDecimal("50.00"))
				.defaultNoticePeriodDays(60)
				.build();

		Unit leaseUnit = unit().id(unitId).build();
		Prop property = prop().id(propertyId).build();

		CreateLeaseRequest request = new CreateLeaseRequest(
				null,
				templateId,
				unitId,
				propertyId,
				LocalDate.of(2026, 4, 1),
				LocalDate.of(2027, 3, 31),
				new BigDecimal("2000.00"),
				1,
				new BigDecimal("2000.00"),
				LateFeeType.PERCENTAGE, // Override
				new BigDecimal("5.00"), // Override
				90, // Override
				null,
				null);

		when(templateService.getEntity(templateId)).thenReturn(template);
		when(unitRepository.findById(unitId)).thenReturn(Optional.of(leaseUnit));
		when(propRepository.findById(propertyId)).thenReturn(Optional.of(property));
		when(leaseRepository.save(any(Lease.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ArgumentCaptor<Lease> leaseCaptor = ArgumentCaptor.forClass(Lease.class);
		leaseService.create(request);

		verify(leaseRepository).save(leaseCaptor.capture());
		Lease savedLease = leaseCaptor.getValue();

		assertThat(savedLease.getLateFeeType()).isEqualTo(LateFeeType.PERCENTAGE);
		assertThat(savedLease.getLateFeeAmount()).isEqualByComparingTo("5.00");
		assertThat(savedLease.getNoticePeriodDays()).isEqualTo(90);
	}

	@Test
	void shouldThrowResourceNotFoundExceptionWhenTemplateNotFound() {
		UUID templateId = UUID.randomUUID();
		CreateLeaseRequest request = lease()
				.leaseTemplate(leaseTemplate().id(templateId).build())
				.buildCreateRequest();

		when(templateService.getEntity(templateId)).thenThrow(
				new ResourceNotFoundException("LeaseTemplate", templateId));

		assertThatThrownBy(() -> leaseService.create(request))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("LeaseTemplate");
	}

	@Test
	void shouldThrowResourceNotFoundExceptionWhenUnitNotFound() {
		UUID templateId = UUID.randomUUID();
		UUID unitId = UUID.randomUUID();

		LeaseTemplate template = leaseTemplate().id(templateId).build();
		CreateLeaseRequest request = lease()
				.leaseTemplate(template)
				.unit(unit().id(unitId).build())
				.buildCreateRequest();

		when(templateService.getEntity(templateId)).thenReturn(template);
		when(unitRepository.findById(unitId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> leaseService.create(request))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Unit");
	}

	@Test
	void shouldThrowResourceNotFoundExceptionWhenPropertyNotFound() {
		UUID templateId = UUID.randomUUID();
		UUID unitId = UUID.randomUUID();
		UUID propertyId = UUID.randomUUID();

		LeaseTemplate template = leaseTemplate().id(templateId).build();
		Unit leaseUnit = unit().id(unitId).build();
		CreateLeaseRequest request = lease()
				.leaseTemplate(template)
				.unit(leaseUnit)
				.property(prop().id(propertyId).build())
				.buildCreateRequest();

		when(templateService.getEntity(templateId)).thenReturn(template);
		when(unitRepository.findById(unitId)).thenReturn(Optional.of(leaseUnit));
		when(propRepository.findById(propertyId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> leaseService.create(request))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Prop");
	}

	// ═══════════════════════════════════════════════════════════════════════
	// Update - DRAFT Only
	// ═══════════════════════════════════════════════════════════════════════

	@Test
	void shouldUpdateDraftLease() {
		UUID leaseId = UUID.randomUUID();
		Lease draftLease = lease()
				.id(leaseId)
				.status(LeaseStatus.DRAFT)
				.rentAmount(new BigDecimal("2000.00"))
				.rentDueDay(1)
				.version(0)
				.build();

		UpdateLeaseRequest request = new UpdateLeaseRequest(
				null, // startDate
				null, // endDate
				new BigDecimal("2200.00"), // Updated rent
				5, // Updated due day
				null, // securityDepositHeld
				null, // lateFeeType
				null, // lateFeeAmount
				null, // noticePeriodDays
				null, // executedContentMarkdown
				null, // additionalMetadata
				null, // templateParameters
				0); // version

		when(leaseRepository.findById(leaseId)).thenReturn(Optional.of(draftLease));
		when(leaseRepository.save(any(Lease.class))).thenAnswer(invocation -> invocation.getArgument(0));

		LeaseResponse response = leaseService.update(leaseId, request);

		assertThat(response.rentAmount()).isEqualByComparingTo("2200.00");
		assertThat(response.rentDueDay()).isEqualTo(5);

		verify(stateMachine).requireDraft(draftLease);
		verify(leaseRepository).save(draftLease);
	}

	@Test
	void shouldNotUpdateNonDraftLease() {
		UUID leaseId = UUID.randomUUID();
		Lease activeLease = lease()
				.id(leaseId)
				.status(LeaseStatus.ACTIVE)
				.version(0)
				.build();

		UpdateLeaseRequest request = new UpdateLeaseRequest(
				null, null, new BigDecimal("2200.00"), null, null, null, null, null, null, null, null, 0);

		when(leaseRepository.findById(leaseId)).thenReturn(Optional.of(activeLease));
		doThrow(new IllegalStateException("Lease " + leaseId + " is ACTIVE; only DRAFT leases can be modified"))
				.when(stateMachine).requireDraft(activeLease);

		assertThatThrownBy(() -> leaseService.update(leaseId, request))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("only DRAFT leases can be modified");

		verify(leaseRepository, never()).save(any());
	}

	@Test
	void shouldThrowOptimisticLockExceptionOnVersionMismatch() {
		UUID leaseId = UUID.randomUUID();
		Lease lease = lease()
				.id(leaseId)
				.status(LeaseStatus.DRAFT)
				.version(5)
				.build();

		UpdateLeaseRequest request = new UpdateLeaseRequest(
				null, null, null, null, null, null, null, null, null, null, null, 3);

		when(leaseRepository.findById(leaseId)).thenReturn(Optional.of(lease));

		assertThatThrownBy(() -> leaseService.update(leaseId, request))
				.isInstanceOf(OptimisticLockException.class)
				.hasMessageContaining("Expected version 3")
				.hasMessageContaining("current version is 5");

		verify(leaseRepository, never()).save(any());
	}

	// ═══════════════════════════════════════════════════════════════════════
	// Status Transitions
	// ═══════════════════════════════════════════════════════════════════════

	@Test
	void shouldSubmitForReview() {
		UUID leaseId = UUID.randomUUID();
		Lease draftLease = lease().id(leaseId).status(LeaseStatus.DRAFT).build();

		when(leaseRepository.findById(leaseId)).thenReturn(Optional.of(draftLease));
		when(leaseRepository.save(draftLease)).thenReturn(draftLease);

		leaseService.submitForReview(leaseId);

		verify(stateMachine).submitForReview(draftLease);
		verify(leaseRepository).save(draftLease);
	}

	@Test
	void shouldActivateAndStampTemplate() {
		UUID leaseId = UUID.randomUUID();
		LeaseTemplate template = leaseTemplate().templateMarkdown("Template {{property_name}}").build();
		Unit leaseUnit = unit().build();
		Prop property = prop().build();
		Lease reviewLease = lease()
				.id(leaseId)
				.status(LeaseStatus.REVIEW)
				.leaseTemplate(template)
				.unit(leaseUnit)
				.property(property)
				.build();

		when(leaseRepository.findById(leaseId)).thenReturn(Optional.of(reviewLease));
		when(leaseRepository.existsByUnit_IdAndStatusAndIdNot(any(), eq(LeaseStatus.ACTIVE), eq(leaseId)))
				.thenReturn(false);
		when(renderer.stampMarkdownFromLease(anyString(), any(), any(), any(), any())).thenReturn("Stamped content");
		when(leaseRepository.save(any(Lease.class))).thenAnswer(invocation -> invocation.getArgument(0));

		LeaseResponse response = leaseService.activate(leaseId);

		verify(stateMachine).activate(reviewLease);
		verify(renderer).stampMarkdownFromLease(anyString(), eq(reviewLease), eq(leaseUnit), eq(property), any());
		assertThat(response.executedContentMarkdown()).isEqualTo("Stamped content");
	}

	@Test
	void shouldThrowWhenActivatingWithExistingActiveLease() {
		UUID leaseId = UUID.randomUUID();
		Unit leaseUnit = unit().build();
		Lease reviewLease = lease()
				.id(leaseId)
				.status(LeaseStatus.REVIEW)
				.unit(leaseUnit)
				.build();

		when(leaseRepository.findById(leaseId)).thenReturn(Optional.of(reviewLease));
		when(leaseRepository.existsByUnit_IdAndStatusAndIdNot(any(), eq(LeaseStatus.ACTIVE), eq(leaseId)))
				.thenReturn(true);

		assertThatThrownBy(() -> leaseService.activate(leaseId))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("unit already has an active lease");

		verify(stateMachine, never()).activate(any());
		verify(leaseRepository, never()).save(any());
	}

	@Test
	void shouldRevertToDraft() {
		UUID leaseId = UUID.randomUUID();
		Lease reviewLease = lease().id(leaseId).status(LeaseStatus.REVIEW).build();

		when(leaseRepository.findById(leaseId)).thenReturn(Optional.of(reviewLease));
		when(leaseRepository.save(reviewLease)).thenReturn(reviewLease);

		leaseService.revertToDraft(leaseId);

		verify(stateMachine).revertToDraft(reviewLease);
		verify(leaseRepository).save(reviewLease);
	}

	@Test
	void shouldTerminate() {
		UUID leaseId = UUID.randomUUID();
		Lease activeLease = lease().id(leaseId).status(LeaseStatus.ACTIVE).build();

		when(leaseRepository.findById(leaseId)).thenReturn(Optional.of(activeLease));
		when(leaseRepository.save(activeLease)).thenReturn(activeLease);

		leaseService.terminate(leaseId);

		verify(stateMachine).terminate(activeLease);
		verify(leaseRepository).save(activeLease);
	}

	// ═══════════════════════════════════════════════════════════════════════
	// Delete - DRAFT Only
	// ═══════════════════════════════════════════════════════════════════════

	@Test
	void shouldDeleteDraftLeaseWhenNoTenants() {
		UUID leaseId = UUID.randomUUID();
		Lease draftLease = lease()
				.id(leaseId)
				.status(LeaseStatus.DRAFT)
				.build();

		when(leaseRepository.findById(leaseId)).thenReturn(Optional.of(draftLease));
		when(leaseTenantRepository.countByLease_Id(leaseId)).thenReturn(0L);

		leaseService.deleteById(leaseId);

		verify(stateMachine).requireDraft(draftLease);
		verify(leaseTenantRepository).countByLease_Id(leaseId);
		verify(leaseRepository).delete(draftLease);
	}

	@Test
	void shouldNotDeleteNonDraftLease() {
		UUID leaseId = UUID.randomUUID();
		Lease activeLease = lease()
				.id(leaseId)
				.status(LeaseStatus.ACTIVE)
				.build();

		when(leaseRepository.findById(leaseId)).thenReturn(Optional.of(activeLease));
		doThrow(new IllegalStateException("only DRAFT leases can be modified"))
				.when(stateMachine).requireDraft(activeLease);

		assertThatThrownBy(() -> leaseService.deleteById(leaseId))
				.isInstanceOf(IllegalStateException.class);

		verify(leaseRepository, never()).delete(any());
	}

	@Test
	void shouldThrowHasChildrenExceptionWhenDeletingLeaseWithTenants() {
		UUID leaseId = UUID.randomUUID();
		Lease draftLease = lease()
				.id(leaseId)
				.status(LeaseStatus.DRAFT)
				.build();

		when(leaseRepository.findById(leaseId)).thenReturn(Optional.of(draftLease));
		when(leaseTenantRepository.countByLease_Id(leaseId)).thenReturn(2L);

		assertThatThrownBy(() -> leaseService.deleteById(leaseId))
				.isInstanceOf(HasChildrenException.class)
				.hasMessageContaining("Cannot delete Lease")
				.hasMessageContaining(leaseId.toString())
				.hasMessageContaining("it has 2 tenant assignment(s)")
				.hasMessageContaining("Remove those first.");

		verify(leaseRepository, never()).delete(any());
	}

	// ═══════════════════════════════════════════════════════════════════════
	// Template Parameter Rendering
	// ═══════════════════════════════════════════════════════════════════════

	@Test
	void shouldStoreTemplateParametersOnLeaseForLaterStamping() {
		UUID templateId = UUID.randomUUID();
		UUID unitId = UUID.randomUUID();
		UUID propertyId = UUID.randomUUID();

		Map<String, String> requestParams = Map.of("special_clause", "Pet allowed");

		LeaseTemplate template = leaseTemplate()
				.id(templateId)
				.templateMarkdown("Template content")
				.templateParameters(Map.of("landlord", "John Doe"))
				.build();

		Unit leaseUnit = unit().id(unitId).build();
		Prop property = prop().id(propertyId).build();

		CreateLeaseRequest request = new CreateLeaseRequest(
				null, templateId, unitId, propertyId,
				LocalDate.now(), LocalDate.now().plusYears(1),
				new BigDecimal("2000.00"), 1, null,
				null, null, null, null, requestParams);

		when(templateService.getEntity(templateId)).thenReturn(template);
		when(unitRepository.findById(unitId)).thenReturn(Optional.of(leaseUnit));
		when(propRepository.findById(propertyId)).thenReturn(Optional.of(property));
		when(leaseRepository.save(any(Lease.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ArgumentCaptor<Lease> leaseCaptor = ArgumentCaptor.forClass(Lease.class);
		leaseService.create(request);

		verify(leaseRepository).save(leaseCaptor.capture());
		// Template params stored on lease for stamping on activate; renderer not called
		// at create
		assertThat(leaseCaptor.getValue().getTemplateParameters()).isEqualTo(requestParams);
		verify(renderer, never()).stampMarkdownFromLease(anyString(), any(), any(), any(), any());
	}
}
