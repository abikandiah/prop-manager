package com.akandiah.propmanager.features.membership.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.akandiah.propmanager.TestDataFactory;
import com.akandiah.propmanager.common.exception.InvalidPermissionStringException;
import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.common.permission.Actions;
import com.akandiah.propmanager.common.permission.ResourceType;
import com.akandiah.propmanager.features.membership.api.dto.CreatePermissionPolicyRequest;
import com.akandiah.propmanager.features.membership.api.dto.PermissionPolicyResponse;
import com.akandiah.propmanager.features.membership.api.dto.UpdatePermissionPolicyRequest;
import com.akandiah.propmanager.features.membership.domain.Membership;
import com.akandiah.propmanager.features.membership.domain.PermissionPolicy;
import com.akandiah.propmanager.features.membership.domain.PermissionPolicyRepository;
import com.akandiah.propmanager.features.membership.domain.PolicyAssignment;
import com.akandiah.propmanager.features.membership.domain.PolicyAssignmentRepository;
import com.akandiah.propmanager.features.organization.domain.Organization;
import com.akandiah.propmanager.features.organization.domain.OrganizationRepository;
import com.akandiah.propmanager.security.OrgGuard;
import com.akandiah.propmanager.security.PermissionGuard;

import jakarta.persistence.OptimisticLockException;

@ExtendWith(MockitoExtension.class)
class PermissionPolicyServiceTest {

	@Mock private PermissionPolicyRepository repository;
	@Mock private OrganizationRepository organizationRepository;
	@Mock private PolicyAssignmentRepository assignmentRepository;
	@Mock private OrgGuard orgGuard;
	@Mock private PermissionGuard permissionGuard;
	@Mock private ApplicationEventPublisher eventPublisher;

	private PermissionPolicyService service;

	@BeforeEach
	void setUp() {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken("admin", null,
						List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
		service = new PermissionPolicyService(
				repository, organizationRepository, assignmentRepository,
				orgGuard, permissionGuard, eventPublisher);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	private void setUserContext() {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken("user", null,
						List.of(new SimpleGrantedAuthority("ROLE_USER"))));
	}

	// --- list ---

	@Test
	void shouldListByOrg() {
		UUID orgId = UUID.randomUUID();
		PermissionPolicy system = policy(null, 0, null, "System Full", Map.of("l", "rcud"));
		PermissionPolicy orgPolicy = policy(null, 0, orgId, "Org Policy", Map.of("l", "ru"));

		when(repository.findByOrgIsNullOrOrg_IdOrderByNameAsc(orgId)).thenReturn(List.of(system, orgPolicy));

		List<PermissionPolicyResponse> result = service.listByOrg(orgId);

		assertThat(result).hasSize(2);
		assertThat(result.get(0).name()).isEqualTo("System Full");
		assertThat(result.get(0).orgId()).isNull();
		assertThat(result.get(1).name()).isEqualTo("Org Policy");
		assertThat(result.get(1).orgId()).isEqualTo(orgId);
	}

	// --- findById ---

	@Test
	void shouldFindById() {
		UUID id = UUID.randomUUID();
		PermissionPolicy p = policy(id, 0, null, "Test", Map.of("l", "r"));
		when(repository.findById(id)).thenReturn(Optional.of(p));

		PermissionPolicyResponse response = service.findById(id);

		assertThat(response.id()).isEqualTo(id);
		assertThat(response.name()).isEqualTo("Test");
		assertThat(response.permissions()).containsEntry("l", "r");
	}

	@Test
	void shouldThrowWhenNotFound() {
		UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.findById(id))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("PermissionPolicy");
	}

	// --- create ---

	@Test
	void shouldCreateSystemPolicy() {
		Map<String, String> perms = Map.of("l", "rcud", "m", "r");
		CreatePermissionPolicyRequest req = new CreatePermissionPolicyRequest(null, "Full Access", perms);

		when(repository.save(any(PermissionPolicy.class))).thenAnswer(inv -> inv.getArgument(0));

		PermissionPolicyResponse response = service.create(req, null);

		ArgumentCaptor<PermissionPolicy> captor = ArgumentCaptor.forClass(PermissionPolicy.class);
		verify(repository).save(captor.capture());
		PermissionPolicy captured = captor.getValue();
		assertThat(captured.getOrg()).isNull();
		assertThat(captured.getName()).isEqualTo("Full Access");
		assertThat(captured.getPermissions()).isEqualTo(perms);
		assertThat(response.name()).isEqualTo("Full Access");
	}

	@Test
	void shouldCreateOrgScopedPolicy() {
		UUID orgId = UUID.randomUUID();
		Organization org = Organization.builder().id(orgId).name("Acme").build();
		Map<String, String> perms = Map.of("l", "cru");
		CreatePermissionPolicyRequest req = new CreatePermissionPolicyRequest(null, "Prop Manager", perms);

		when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
		when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		PermissionPolicyResponse response = service.create(req, orgId);

		ArgumentCaptor<PermissionPolicy> captor = ArgumentCaptor.forClass(PermissionPolicy.class);
		verify(repository).save(captor.capture());
		assertThat(captor.getValue().getOrg().getId()).isEqualTo(orgId);
		assertThat(response.orgId()).isEqualTo(orgId);
	}

	@Test
	void shouldThrowWhenCreatingWithInvalidPermissions() {
		Map<String, String> perms = Map.of("x", "r"); // unknown domain key
		CreatePermissionPolicyRequest req = new CreatePermissionPolicyRequest(null, "Bad", perms);

		assertThatThrownBy(() -> service.create(req, null))
				.isInstanceOf(InvalidPermissionStringException.class);

		verify(repository, never()).save(any());
	}

	@Test
	void create_systemPolicy_deniesNonAdmin() {
		setUserContext();
		Map<String, String> perms = Map.of("l", "r");
		CreatePermissionPolicyRequest req = new CreatePermissionPolicyRequest(null, "System", perms);

		assertThatThrownBy(() -> service.create(req, null))
				.isInstanceOf(AccessDeniedException.class);

		verify(repository, never()).save(any());
	}

	// --- update ---

	@Test
	void shouldUpdatePolicyNameAndPermissions() {
		UUID id = UUID.randomUUID();
		PermissionPolicy existing = policy(id, 1, null, "Old", Map.of("l", "r"));
		Map<String, String> newPerms = Map.of("l", "rcud");
		UpdatePermissionPolicyRequest req = new UpdatePermissionPolicyRequest("New Name", newPerms, 1);

		when(repository.findById(id)).thenReturn(Optional.of(existing));
		when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(assignmentRepository.findByPolicyId(id)).thenReturn(List.of());

		PermissionPolicyResponse response = service.update(id, req, null);

		assertThat(response.name()).isEqualTo("New Name");
		assertThat(response.permissions()).isEqualTo(newPerms);
	}

	@Test
	void update_evictsCacheForLinkedMemberships() {
		UUID id = UUID.randomUUID();
		PermissionPolicy existing = policy(id, 0, null, "T", Map.of("l", "r"));

		com.akandiah.propmanager.features.user.domain.User user = TestDataFactory.user().build();
		Membership linkedMembership = Membership.builder()
				.id(UUID.randomUUID()).user(user).build();
		PolicyAssignment assignment = PolicyAssignment.builder()
				.id(UUID.randomUUID()).membership(linkedMembership)
				.resourceType(ResourceType.ORG).resourceId(UUID.randomUUID())
				.policy(existing).build();

		Map<String, String> newPerms = Map.of("l", "rcud");
		UpdatePermissionPolicyRequest req = new UpdatePermissionPolicyRequest(null, newPerms, 0);

		when(repository.findById(id)).thenReturn(Optional.of(existing));
		when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(assignmentRepository.findByPolicyId(id)).thenReturn(List.of(assignment));

		service.update(id, req, null);

		verify(eventPublisher).publishEvent(any(
				com.akandiah.propmanager.features.auth.domain.PermissionsChangedEvent.class));
	}

	@Test
	void shouldThrowOnVersionMismatch() {
		UUID id = UUID.randomUUID();
		PermissionPolicy existing = policy(id, 2, null, "T", Map.of("l", "r"));
		UpdatePermissionPolicyRequest req = new UpdatePermissionPolicyRequest("T", null, 1);

		when(repository.findById(id)).thenReturn(Optional.of(existing));

		assertThatThrownBy(() -> service.update(id, req, null))
				.isInstanceOf(OptimisticLockException.class);

		verify(repository, never()).save(any());
	}

	@Test
	void update_orgPolicy_allowsOrgAdmin() {
		setUserContext();
		UUID orgId = UUID.randomUUID();
		UUID id = UUID.randomUUID();
		PermissionPolicy existing = policy(id, 0, orgId, "T", Map.of("l", "r"));

		when(repository.findById(id)).thenReturn(Optional.of(existing));
		when(permissionGuard.hasAccess(Actions.UPDATE, "o", ResourceType.ORG, orgId, orgId)).thenReturn(true);
		when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		PermissionPolicyResponse response = service.update(id,
				new UpdatePermissionPolicyRequest("New", null, 0), orgId);

		assertThat(response.name()).isEqualTo("New");
	}

	@Test
	void update_orgPolicy_deniesRegularMember() {
		setUserContext();
		UUID orgId = UUID.randomUUID();
		UUID id = UUID.randomUUID();
		PermissionPolicy existing = policy(id, 0, orgId, "T", Map.of("l", "r"));

		when(repository.findById(id)).thenReturn(Optional.of(existing));
		when(permissionGuard.hasAccess(Actions.UPDATE, "o", ResourceType.ORG, orgId, orgId)).thenReturn(false);

		assertThatThrownBy(() -> service.update(id, new UpdatePermissionPolicyRequest("T", null, 0), orgId))
				.isInstanceOf(AccessDeniedException.class);

		verify(repository, never()).save(any());
	}

	// --- delete ---

	@Test
	void shouldDeleteById() {
		UUID id = UUID.randomUUID();
		PermissionPolicy p = policy(id, 0, null, "T", Map.of("l", "r"));
		when(repository.findById(id)).thenReturn(Optional.of(p));
		when(assignmentRepository.findByPolicyId(id)).thenReturn(List.of());

		service.deleteById(id, null);

		verify(repository).delete(p);
	}

	@Test
	void shouldThrowWhenDeletingNonExistent() {
		UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.deleteById(id, null))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("PermissionPolicy");

		verify(repository, never()).delete(any());
	}

	@Test
	void delete_systemPolicy_deniesNonAdmin() {
		setUserContext();
		UUID id = UUID.randomUUID();
		PermissionPolicy p = policy(id, 0, null, "T", Map.of("l", "r"));
		when(repository.findById(id)).thenReturn(Optional.of(p));

		assertThatThrownBy(() -> service.deleteById(id, null))
				.isInstanceOf(AccessDeniedException.class);

		verify(repository, never()).delete(any());
	}

	// --- helpers ---

	private static PermissionPolicy policy(UUID id, int version, UUID orgId, String name,
			Map<String, String> permissions) {
		Organization org = orgId != null
				? Organization.builder().id(orgId).name("Org").build()
				: null;
		return PermissionPolicy.builder()
				.id(id)
				.org(org)
				.name(name)
				.permissions(new HashMap<>(permissions))
				.version(version)
				.createdAt(Instant.now())
				.updatedAt(Instant.now())
				.build();
	}
}
