package com.akandiah.propmanager.features.membership.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.OptimisticLockException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.akandiah.propmanager.TestDataFactory;
import com.akandiah.propmanager.common.exception.InvalidPermissionStringException;
import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.common.permission.ResourceType;
import com.akandiah.propmanager.features.asset.domain.AssetRepository;
import com.akandiah.propmanager.features.auth.domain.PermissionsChangedEvent;
import com.akandiah.propmanager.features.membership.api.dto.CreatePolicyAssignmentRequest;
import com.akandiah.propmanager.features.membership.api.dto.UpdatePolicyAssignmentRequest;
import com.akandiah.propmanager.features.membership.domain.Membership;
import com.akandiah.propmanager.features.membership.domain.MembershipRepository;
import com.akandiah.propmanager.features.membership.domain.PermissionPolicyRepository;
import com.akandiah.propmanager.features.membership.domain.PolicyAssignment;
import com.akandiah.propmanager.features.membership.domain.PolicyAssignmentRepository;
import com.akandiah.propmanager.features.organization.domain.Organization;
import com.akandiah.propmanager.features.prop.domain.PropRepository;
import com.akandiah.propmanager.features.unit.domain.UnitRepository;
import com.akandiah.propmanager.features.user.domain.User;

import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class PolicyAssignmentServiceTest {

	@Mock private PolicyAssignmentRepository assignmentRepository;
	@Mock private MembershipRepository membershipRepository;
	@Mock private PermissionPolicyRepository policyRepository;
	@Mock private PropRepository propRepository;
	@Mock private UnitRepository unitRepository;
	@Mock private AssetRepository assetRepository;
	@Mock private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private PolicyAssignmentService service;

	// ─── create() ───────────────────────────────────────────────────────

	@Test
	void create_setsOverridesWhenProvided() {
		UUID membershipId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		Membership membership = membershipWithUser(membershipId);
		Map<String, String> overrides = Map.of("l", "rcud");
		CreatePolicyAssignmentRequest req = new CreatePolicyAssignmentRequest(
				null, ResourceType.PROPERTY, resourceId, null, overrides);

		when(membershipRepository.findById(membershipId)).thenReturn(Optional.of(membership));
		when(propRepository.existsByIdAndOrganization_Id(resourceId, membership.getOrganization().getId()))
				.thenReturn(true);
		when(assignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		var result = service.create(membershipId, req);

		ArgumentCaptor<PolicyAssignment> captor = ArgumentCaptor.forClass(PolicyAssignment.class);
		verify(assignmentRepository).save(captor.capture());
		assertThat(captor.getValue().getOverrides()).isEqualTo(overrides);
		assertThat(result.overrides()).isEqualTo(overrides);
	}

	@Test
	void create_withPolicyIdAndNoOverrides() {
		UUID membershipId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		UUID policyId = UUID.randomUUID();
		Membership membership = membershipWithUser(membershipId);
		CreatePolicyAssignmentRequest req = new CreatePolicyAssignmentRequest(
				null, ResourceType.PROPERTY, resourceId, policyId, null);

		when(membershipRepository.findById(membershipId)).thenReturn(Optional.of(membership));
		when(propRepository.existsByIdAndOrganization_Id(resourceId, membership.getOrganization().getId()))
				.thenReturn(true);
		com.akandiah.propmanager.features.membership.domain.PermissionPolicy policy =
				com.akandiah.propmanager.features.membership.domain.PermissionPolicy.builder()
						.id(policyId).name("Policy").build();
		when(policyRepository.findById(policyId)).thenReturn(Optional.of(policy));
		when(assignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		var result = service.create(membershipId, req);

		ArgumentCaptor<PolicyAssignment> captor = ArgumentCaptor.forClass(PolicyAssignment.class);
		verify(assignmentRepository).save(captor.capture());
		assertThat(captor.getValue().getPolicy()).isNotNull();
		assertThat(result.policyId()).isEqualTo(policyId);
	}

	@Test
	void create_throwsWhenOverridesInvalid() {
		UUID membershipId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		Membership membership = membershipWithUser(membershipId);
		CreatePolicyAssignmentRequest req = new CreatePolicyAssignmentRequest(
				null, ResourceType.PROPERTY, resourceId, null, Map.of("l", "x")); // invalid letter

		when(membershipRepository.findById(membershipId)).thenReturn(Optional.of(membership));
		when(propRepository.existsByIdAndOrganization_Id(resourceId, membership.getOrganization().getId()))
				.thenReturn(true);

		assertThatThrownBy(() -> service.create(membershipId, req))
				.isInstanceOf(InvalidPermissionStringException.class);
	}

	// ─── create() — resource type validation ────────────────────────────

	@Test
	void create_validatesOrgResourceMatchesOrgId() {
		UUID membershipId = UUID.randomUUID();
		Membership membership = membershipWithUser(membershipId);
		UUID orgId = membership.getOrganization().getId();
		CreatePolicyAssignmentRequest req = new CreatePolicyAssignmentRequest(
				null, ResourceType.ORG, orgId, null, Map.of());

		when(membershipRepository.findById(membershipId)).thenReturn(Optional.of(membership));
		when(assignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		var result = service.create(membershipId, req);

		assertThat(result.resourceType()).isEqualTo(ResourceType.ORG);
	}

	@Test
	void create_throwsWhenOrgResourceIdDoesNotMatchOrg() {
		UUID membershipId = UUID.randomUUID();
		Membership membership = membershipWithUser(membershipId);
		UUID wrongOrgId = UUID.randomUUID();
		CreatePolicyAssignmentRequest req = new CreatePolicyAssignmentRequest(
				null, ResourceType.ORG, wrongOrgId, null, Map.of());

		when(membershipRepository.findById(membershipId)).thenReturn(Optional.of(membership));

		assertThatThrownBy(() -> service.create(membershipId, req))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void create_validatesUnitResourceBelongsToOrg() {
		UUID membershipId = UUID.randomUUID();
		UUID unitId = UUID.randomUUID();
		Membership membership = membershipWithUser(membershipId);
		UUID orgId = membership.getOrganization().getId();
		CreatePolicyAssignmentRequest req = new CreatePolicyAssignmentRequest(
				null, ResourceType.UNIT, unitId, null, Map.of());

		when(membershipRepository.findById(membershipId)).thenReturn(Optional.of(membership));
		when(unitRepository.existsByIdAndProp_Organization_Id(unitId, orgId)).thenReturn(true);
		when(assignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		var result = service.create(membershipId, req);

		assertThat(result.resourceType()).isEqualTo(ResourceType.UNIT);
	}

	@Test
	void create_throwsWhenUnitDoesNotBelongToOrg() {
		UUID membershipId = UUID.randomUUID();
		UUID unitId = UUID.randomUUID();
		Membership membership = membershipWithUser(membershipId);
		UUID orgId = membership.getOrganization().getId();
		CreatePolicyAssignmentRequest req = new CreatePolicyAssignmentRequest(
				null, ResourceType.UNIT, unitId, null, Map.of());

		when(membershipRepository.findById(membershipId)).thenReturn(Optional.of(membership));
		when(unitRepository.existsByIdAndProp_Organization_Id(unitId, orgId)).thenReturn(false);

		assertThatThrownBy(() -> service.create(membershipId, req))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	// ─── create() — null user (pending invite) ──────────────────────────

	@Test
	void create_succeedsWithNullUserAndSkipsEvent() {
		UUID membershipId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		Membership membership = membershipWithoutUser(membershipId);
		CreatePolicyAssignmentRequest req = new CreatePolicyAssignmentRequest(
				null, ResourceType.PROPERTY, resourceId, null, Map.of());

		when(membershipRepository.findById(membershipId)).thenReturn(Optional.of(membership));
		when(propRepository.existsByIdAndOrganization_Id(resourceId, membership.getOrganization().getId()))
				.thenReturn(true);
		when(assignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		service.create(membershipId, req);

		verify(assignmentRepository).save(any());
		verify(eventPublisher, never()).publishEvent(any(PermissionsChangedEvent.class));
	}

	// ─── update() ───────────────────────────────────────────────────────

	@Test
	void update_updatesOverridesAndPublishesEvent() {
		UUID membershipId = UUID.randomUUID();
		UUID assignmentId = UUID.randomUUID();
		User user = TestDataFactory.user().build();
		Organization org = TestDataFactory.organization().build();
		Membership membership = Membership.builder()
				.id(membershipId).user(user).organization(org).build();
		PolicyAssignment assignment = PolicyAssignment.builder()
				.id(assignmentId).membership(membership)
				.resourceType(ResourceType.PROPERTY).resourceId(UUID.randomUUID())
				.overrides(Map.of("l", "r")).version(0).build();
		Map<String, String> newOverrides = Map.of("l", "rcud");
		UpdatePolicyAssignmentRequest req = new UpdatePolicyAssignmentRequest(null, newOverrides, 0);

		when(assignmentRepository.findByIdAndMembershipId(assignmentId, membershipId))
				.thenReturn(Optional.of(assignment));
		when(assignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		var result = service.update(membershipId, assignmentId, req);

		assertThat(result.overrides()).isEqualTo(newOverrides);
		verify(eventPublisher).publishEvent(any(PermissionsChangedEvent.class));
	}

	@Test
	void update_throwsOnVersionMismatch() {
		UUID membershipId = UUID.randomUUID();
		UUID assignmentId = UUID.randomUUID();
		Organization org = TestDataFactory.organization().build();
		Membership membership = Membership.builder()
				.id(membershipId).organization(org).build();
		PolicyAssignment assignment = PolicyAssignment.builder()
				.id(assignmentId).membership(membership)
				.resourceType(ResourceType.ORG).resourceId(org.getId())
				.version(2).build();
		UpdatePolicyAssignmentRequest req = new UpdatePolicyAssignmentRequest(null, Map.of(), 0);

		when(assignmentRepository.findByIdAndMembershipId(assignmentId, membershipId))
				.thenReturn(Optional.of(assignment));

		assertThatThrownBy(() -> service.update(membershipId, assignmentId, req))
				.isInstanceOf(OptimisticLockException.class);
	}

	@Test
	void update_skipsEventWhenUserIsNull() {
		UUID membershipId = UUID.randomUUID();
		UUID assignmentId = UUID.randomUUID();
		Organization org = TestDataFactory.organization().build();
		Membership membership = Membership.builder()
				.id(membershipId).user(null).organization(org).build();
		PolicyAssignment assignment = PolicyAssignment.builder()
				.id(assignmentId).membership(membership)
				.resourceType(ResourceType.ORG).resourceId(org.getId())
				.version(0).build();
		UpdatePolicyAssignmentRequest req = new UpdatePolicyAssignmentRequest(null, Map.of("l", "r"), 0);

		when(assignmentRepository.findByIdAndMembershipId(assignmentId, membershipId))
				.thenReturn(Optional.of(assignment));
		when(assignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		service.update(membershipId, assignmentId, req);

		verify(assignmentRepository).save(any());
		verify(eventPublisher, never()).publishEvent(any(PermissionsChangedEvent.class));
	}

	@Test
	void update_throwsWhenAssignmentNotFound() {
		UUID membershipId = UUID.randomUUID();
		UUID assignmentId = UUID.randomUUID();
		UpdatePolicyAssignmentRequest req = new UpdatePolicyAssignmentRequest(null, Map.of(), 0);

		when(assignmentRepository.findByIdAndMembershipId(assignmentId, membershipId))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(membershipId, assignmentId, req))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	// ─── deleteById() ───────────────────────────────────────────────────

	@Test
	void deleteById_deletesAndPublishesEventForActiveUser() {
		UUID membershipId = UUID.randomUUID();
		UUID assignmentId = UUID.randomUUID();
		User user = TestDataFactory.user().build();
		Organization org = TestDataFactory.organization().build();
		Membership membership = Membership.builder()
				.id(membershipId).user(user).organization(org).build();
		PolicyAssignment assignment = PolicyAssignment.builder()
				.id(assignmentId).membership(membership)
				.resourceType(ResourceType.ORG).resourceId(org.getId())
				.build();

		when(assignmentRepository.findByIdAndMembershipId(assignmentId, membershipId))
				.thenReturn(Optional.of(assignment));

		service.deleteById(membershipId, assignmentId);

		verify(assignmentRepository).deleteById(assignmentId);
		ArgumentCaptor<PermissionsChangedEvent> captor = ArgumentCaptor.forClass(PermissionsChangedEvent.class);
		verify(eventPublisher).publishEvent(captor.capture());
		assertThat(captor.getValue().affectedUserIds()).isEqualTo(Set.of(user.getId()));
	}

	@Test
	void deleteById_skipsEventWhenUserIsNull() {
		UUID membershipId = UUID.randomUUID();
		UUID assignmentId = UUID.randomUUID();
		Organization org = TestDataFactory.organization().build();
		Membership membership = Membership.builder()
				.id(membershipId).user(null).organization(org).build();
		PolicyAssignment assignment = PolicyAssignment.builder()
				.id(assignmentId).membership(membership)
				.resourceType(ResourceType.ORG).resourceId(org.getId())
				.build();

		when(assignmentRepository.findByIdAndMembershipId(assignmentId, membershipId))
				.thenReturn(Optional.of(assignment));

		service.deleteById(membershipId, assignmentId);

		verify(assignmentRepository).deleteById(assignmentId);
		verify(eventPublisher, never()).publishEvent(any(PermissionsChangedEvent.class));
	}

	@Test
	void deleteById_throwsWhenAssignmentNotFound() {
		UUID membershipId = UUID.randomUUID();
		UUID assignmentId = UUID.randomUUID();

		when(assignmentRepository.findByIdAndMembershipId(assignmentId, membershipId))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.deleteById(membershipId, assignmentId))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	// ─── helpers ────────────────────────────────────────────────────────

	private static Membership membershipWithUser(UUID id) {
		User user = TestDataFactory.user().build();
		Organization org = TestDataFactory.organization().build();
		return Membership.builder()
				.id(id).user(user).organization(org).build();
	}

	private static Membership membershipWithoutUser(UUID id) {
		Organization org = TestDataFactory.organization().build();
		return Membership.builder()
				.id(id).user(null).organization(org).build();
	}
}
