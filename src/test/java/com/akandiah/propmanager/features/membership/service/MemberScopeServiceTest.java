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
import com.akandiah.propmanager.features.auth.domain.PermissionsChangedEvent;
import com.akandiah.propmanager.features.membership.api.dto.CreateMemberScopeRequest;
import com.akandiah.propmanager.features.membership.api.dto.UpdateMemberScopeRequest;
import com.akandiah.propmanager.features.membership.domain.MemberScope;
import com.akandiah.propmanager.features.membership.domain.MemberScopeRepository;
import com.akandiah.propmanager.features.membership.domain.Membership;
import com.akandiah.propmanager.features.membership.domain.MembershipRepository;
import com.akandiah.propmanager.features.organization.domain.Organization;
import com.akandiah.propmanager.features.prop.domain.PropRepository;
import com.akandiah.propmanager.features.unit.domain.UnitRepository;
import com.akandiah.propmanager.features.user.domain.User;

import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class MemberScopeServiceTest {

	@Mock private MemberScopeRepository memberScopeRepository;
	@Mock private MembershipRepository membershipRepository;
	@Mock private PropRepository propRepository;
	@Mock private UnitRepository unitRepository;
	@Mock private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private MemberScopeService service;

	// ─── create() ───────────────────────────────────────────────────────

	@Test
	void create_setsPermissionsWhenProvided() {
		UUID membershipId = UUID.randomUUID();
		UUID scopeId = UUID.randomUUID();
		Membership membership = membershipWithUser(membershipId);
		Map<String, String> perms = Map.of("l", "rcud");
		CreateMemberScopeRequest req = new CreateMemberScopeRequest(ResourceType.PROPERTY, scopeId, perms);

		when(membershipRepository.findById(membershipId)).thenReturn(Optional.of(membership));
		when(propRepository.existsByIdAndOrganization_Id(scopeId, membership.getOrganization().getId()))
				.thenReturn(true);
		when(memberScopeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		var result = service.create(membershipId, req);

		ArgumentCaptor<MemberScope> captor = ArgumentCaptor.forClass(MemberScope.class);
		verify(memberScopeRepository).save(captor.capture());
		assertThat(captor.getValue().getPermissions()).isEqualTo(perms);
		assertThat(result.permissions()).isEqualTo(perms);
	}

	@Test
	void create_usesEmptyPermissionsWhenNullProvided() {
		UUID membershipId = UUID.randomUUID();
		UUID scopeId = UUID.randomUUID();
		Membership membership = membershipWithUser(membershipId);
		CreateMemberScopeRequest req = new CreateMemberScopeRequest(ResourceType.PROPERTY, scopeId, null);

		when(membershipRepository.findById(membershipId)).thenReturn(Optional.of(membership));
		when(propRepository.existsByIdAndOrganization_Id(scopeId, membership.getOrganization().getId()))
				.thenReturn(true);
		when(memberScopeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		var result = service.create(membershipId, req);

		ArgumentCaptor<MemberScope> captor = ArgumentCaptor.forClass(MemberScope.class);
		verify(memberScopeRepository).save(captor.capture());
		assertThat(captor.getValue().getPermissions()).isEmpty();
		assertThat(result.permissions()).isEmpty();
	}

	@Test
	void create_throwsWhenPermissionsInvalid() {
		UUID membershipId = UUID.randomUUID();
		UUID scopeId = UUID.randomUUID();
		Membership membership = membershipWithUser(membershipId);
		CreateMemberScopeRequest req = new CreateMemberScopeRequest(
				ResourceType.PROPERTY, scopeId, Map.of("l", "x")); // invalid letter

		when(membershipRepository.findById(membershipId)).thenReturn(Optional.of(membership));
		when(propRepository.existsByIdAndOrganization_Id(scopeId, membership.getOrganization().getId()))
				.thenReturn(true);

		assertThatThrownBy(() -> service.create(membershipId, req))
				.isInstanceOf(InvalidPermissionStringException.class);
	}

	// ─── create() — scope type validation ───────────────────────────────

	@Test
	void create_validatesOrgScopeMatchesOrgId() {
		UUID membershipId = UUID.randomUUID();
		Membership membership = membershipWithUser(membershipId);
		UUID orgId = membership.getOrganization().getId();
		CreateMemberScopeRequest req = new CreateMemberScopeRequest(ResourceType.ORG, orgId, Map.of());

		when(membershipRepository.findById(membershipId)).thenReturn(Optional.of(membership));
		when(memberScopeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		var result = service.create(membershipId, req);

		assertThat(result.scopeType()).isEqualTo(ResourceType.ORG);
	}

	@Test
	void create_throwsWhenOrgScopeIdDoesNotMatchOrg() {
		UUID membershipId = UUID.randomUUID();
		Membership membership = membershipWithUser(membershipId);
		UUID wrongOrgId = UUID.randomUUID();
		CreateMemberScopeRequest req = new CreateMemberScopeRequest(ResourceType.ORG, wrongOrgId, Map.of());

		when(membershipRepository.findById(membershipId)).thenReturn(Optional.of(membership));

		assertThatThrownBy(() -> service.create(membershipId, req))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void create_validatesUnitScopeBelongsToOrg() {
		UUID membershipId = UUID.randomUUID();
		UUID unitId = UUID.randomUUID();
		Membership membership = membershipWithUser(membershipId);
		UUID orgId = membership.getOrganization().getId();
		CreateMemberScopeRequest req = new CreateMemberScopeRequest(ResourceType.UNIT, unitId, Map.of());

		when(membershipRepository.findById(membershipId)).thenReturn(Optional.of(membership));
		when(unitRepository.existsByIdAndProp_Organization_Id(unitId, orgId)).thenReturn(true);
		when(memberScopeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		var result = service.create(membershipId, req);

		assertThat(result.scopeType()).isEqualTo(ResourceType.UNIT);
	}

	@Test
	void create_throwsWhenUnitDoesNotBelongToOrg() {
		UUID membershipId = UUID.randomUUID();
		UUID unitId = UUID.randomUUID();
		Membership membership = membershipWithUser(membershipId);
		UUID orgId = membership.getOrganization().getId();
		CreateMemberScopeRequest req = new CreateMemberScopeRequest(ResourceType.UNIT, unitId, Map.of());

		when(membershipRepository.findById(membershipId)).thenReturn(Optional.of(membership));
		when(unitRepository.existsByIdAndProp_Organization_Id(unitId, orgId)).thenReturn(false);

		assertThatThrownBy(() -> service.create(membershipId, req))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	// ─── create() — null user (pending invite / binding row) ────────────

	@Test
	void create_succeedsWithNullUserAndSkipsEvent() {
		UUID membershipId = UUID.randomUUID();
		UUID scopeId = UUID.randomUUID();
		Membership membership = membershipWithoutUser(membershipId);
		CreateMemberScopeRequest req = new CreateMemberScopeRequest(ResourceType.PROPERTY, scopeId, Map.of());

		when(membershipRepository.findById(membershipId)).thenReturn(Optional.of(membership));
		when(propRepository.existsByIdAndOrganization_Id(scopeId, membership.getOrganization().getId()))
				.thenReturn(true);
		when(memberScopeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		service.create(membershipId, req);

		verify(memberScopeRepository).save(any());
		verify(eventPublisher, never()).publishEvent(any(PermissionsChangedEvent.class));
	}

	// ─── update() ───────────────────────────────────────────────────────

	@Test
	void update_updatesPermissionsAndPublishesEvent() {
		UUID membershipId = UUID.randomUUID();
		UUID scopeId = UUID.randomUUID();
		User user = TestDataFactory.user().build();
		Organization org = TestDataFactory.organization().build();
		Membership membership = Membership.builder()
				.id(membershipId).user(user).organization(org).version(0).build();
		MemberScope scope = MemberScope.builder()
				.id(scopeId).membership(membership)
				.scopeType(ResourceType.PROPERTY).scopeId(UUID.randomUUID())
				.permissions(Map.of("l", "r")).version(0).build();
		Map<String, String> newPerms = Map.of("l", "rcud");
		UpdateMemberScopeRequest req = new UpdateMemberScopeRequest(newPerms, 0);

		when(memberScopeRepository.findByIdAndMembershipId(scopeId, membershipId))
				.thenReturn(Optional.of(scope));
		when(memberScopeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		var result = service.update(membershipId, scopeId, req);

		assertThat(result.permissions()).isEqualTo(newPerms);
		verify(eventPublisher).publishEvent(any(PermissionsChangedEvent.class));
	}

	@Test
	void update_throwsOnVersionMismatch() {
		UUID membershipId = UUID.randomUUID();
		UUID scopeId = UUID.randomUUID();
		Organization org = TestDataFactory.organization().build();
		Membership membership = Membership.builder()
				.id(membershipId).organization(org).version(0).build();
		MemberScope scope = MemberScope.builder()
				.id(scopeId).membership(membership)
				.scopeType(ResourceType.ORG).scopeId(org.getId())
				.permissions(Map.of()).version(2).build();
		UpdateMemberScopeRequest req = new UpdateMemberScopeRequest(Map.of(), 0);

		when(memberScopeRepository.findByIdAndMembershipId(scopeId, membershipId))
				.thenReturn(Optional.of(scope));

		assertThatThrownBy(() -> service.update(membershipId, scopeId, req))
				.isInstanceOf(OptimisticLockException.class);
	}

	@Test
	void update_skipsEventWhenUserIsNull() {
		UUID membershipId = UUID.randomUUID();
		UUID scopeId = UUID.randomUUID();
		Organization org = TestDataFactory.organization().build();
		Membership membership = Membership.builder()
				.id(membershipId).user(null).organization(org).version(0).build();
		MemberScope scope = MemberScope.builder()
				.id(scopeId).membership(membership)
				.scopeType(ResourceType.ORG).scopeId(org.getId())
				.permissions(Map.of()).version(0).build();
		UpdateMemberScopeRequest req = new UpdateMemberScopeRequest(Map.of("l", "r"), 0);

		when(memberScopeRepository.findByIdAndMembershipId(scopeId, membershipId))
				.thenReturn(Optional.of(scope));
		when(memberScopeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		service.update(membershipId, scopeId, req);

		verify(memberScopeRepository).save(any());
		verify(eventPublisher, never()).publishEvent(any(PermissionsChangedEvent.class));
	}

	@Test
	void update_throwsWhenScopeNotFound() {
		UUID membershipId = UUID.randomUUID();
		UUID scopeId = UUID.randomUUID();
		UpdateMemberScopeRequest req = new UpdateMemberScopeRequest(Map.of(), 0);

		when(memberScopeRepository.findByIdAndMembershipId(scopeId, membershipId))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(membershipId, scopeId, req))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	// ─── deleteById() ───────────────────────────────────────────────────

	@Test
	void deleteById_deletesAndPublishesEventForActiveUser() {
		UUID membershipId = UUID.randomUUID();
		UUID scopeId = UUID.randomUUID();
		User user = TestDataFactory.user().build();
		Organization org = TestDataFactory.organization().build();
		Membership membership = Membership.builder()
				.id(membershipId).user(user).organization(org).version(0).build();
		MemberScope scope = MemberScope.builder()
				.id(scopeId).membership(membership)
				.scopeType(ResourceType.ORG).scopeId(org.getId())
				.permissions(Map.of()).version(0).build();

		when(memberScopeRepository.findByIdAndMembershipId(scopeId, membershipId))
				.thenReturn(Optional.of(scope));

		service.deleteById(membershipId, scopeId);

		verify(memberScopeRepository).deleteById(scopeId);
		ArgumentCaptor<PermissionsChangedEvent> captor = ArgumentCaptor.forClass(PermissionsChangedEvent.class);
		verify(eventPublisher).publishEvent(captor.capture());
		assertThat(captor.getValue().affectedUserIds()).isEqualTo(Set.of(user.getId()));
	}

	@Test
	void deleteById_skipsEventWhenUserIsNull() {
		UUID membershipId = UUID.randomUUID();
		UUID scopeId = UUID.randomUUID();
		Organization org = TestDataFactory.organization().build();
		Membership membership = Membership.builder()
				.id(membershipId).user(null).organization(org).version(0).build();
		MemberScope scope = MemberScope.builder()
				.id(scopeId).membership(membership)
				.scopeType(ResourceType.ORG).scopeId(org.getId())
				.permissions(Map.of()).version(0).build();

		when(memberScopeRepository.findByIdAndMembershipId(scopeId, membershipId))
				.thenReturn(Optional.of(scope));

		service.deleteById(membershipId, scopeId);

		verify(memberScopeRepository).deleteById(scopeId);
		verify(eventPublisher, never()).publishEvent(any(PermissionsChangedEvent.class));
	}

	@Test
	void deleteById_throwsWhenScopeNotFound() {
		UUID membershipId = UUID.randomUUID();
		UUID scopeId = UUID.randomUUID();

		when(memberScopeRepository.findByIdAndMembershipId(scopeId, membershipId))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.deleteById(membershipId, scopeId))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	// ─── helpers ────────────────────────────────────────────────────────

	private static Membership membershipWithUser(UUID id) {
		User user = TestDataFactory.user().build();
		Organization org = TestDataFactory.organization().build();
		return Membership.builder()
				.id(id).user(user).organization(org).version(0).build();
	}

	private static Membership membershipWithoutUser(UUID id) {
		Organization org = TestDataFactory.organization().build();
		return Membership.builder()
				.id(id).user(null).organization(org).version(0).build();
	}
}
