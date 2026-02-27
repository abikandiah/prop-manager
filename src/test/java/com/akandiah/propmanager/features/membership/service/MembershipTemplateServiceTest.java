package com.akandiah.propmanager.features.membership.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
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
import com.akandiah.propmanager.features.membership.api.dto.CreateMembershipTemplateRequest;
import com.akandiah.propmanager.features.membership.api.dto.MembershipTemplateItemView;
import com.akandiah.propmanager.features.membership.api.dto.MembershipTemplateResponse;
import com.akandiah.propmanager.features.membership.api.dto.UpdateMembershipTemplateRequest;
import com.akandiah.propmanager.features.membership.domain.Membership;
import com.akandiah.propmanager.features.membership.domain.MembershipRepository;
import com.akandiah.propmanager.features.membership.domain.MembershipTemplate;
import com.akandiah.propmanager.features.membership.domain.MembershipTemplateItem;
import com.akandiah.propmanager.features.membership.domain.MembershipTemplateRepository;
import com.akandiah.propmanager.features.organization.domain.Organization;
import com.akandiah.propmanager.features.organization.domain.OrganizationRepository;
import com.akandiah.propmanager.security.OrgGuard;
import com.akandiah.propmanager.security.PermissionGuard;

import jakarta.persistence.OptimisticLockException;

@ExtendWith(MockitoExtension.class)
class MembershipTemplateServiceTest {

	@Mock private MembershipTemplateRepository repository;
	@Mock private OrganizationRepository organizationRepository;
	@Mock private MembershipRepository membershipRepository;
	@Mock private OrgGuard orgGuard;
	@Mock private PermissionGuard permissionGuard;
	@Mock private ApplicationEventPublisher eventPublisher;

	private MembershipTemplateService service;

	@BeforeEach
	void setUp() {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken("admin", null,
						List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
		service = new MembershipTemplateService(
				repository, organizationRepository, membershipRepository,
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
		MembershipTemplate system = template(null, 0, null, "System Full", orgItem(Map.of("l", "rcud")));
		MembershipTemplate orgTpl = template(null, 0, orgId, "Org Template", propItem(Map.of("l", "ru")));

		when(repository.findByOrgIsNullOrOrg_IdOrderByNameAsc(orgId)).thenReturn(List.of(system, orgTpl));

		List<MembershipTemplateResponse> result = service.listByOrg(orgId);

		assertThat(result).hasSize(2);
		assertThat(result.get(0).name()).isEqualTo("System Full");
		assertThat(result.get(0).orgId()).isNull();
		assertThat(result.get(1).name()).isEqualTo("Org Template");
		assertThat(result.get(1).orgId()).isEqualTo(orgId);
	}

	// --- findById ---

	@Test
	void shouldFindById() {
		UUID id = UUID.randomUUID();
		MembershipTemplate t = template(id, 0, null, "Test", orgItem(Map.of("l", "r")));
		when(repository.findById(id)).thenReturn(Optional.of(t));

		MembershipTemplateResponse response = service.findById(id);

		assertThat(response.id()).isEqualTo(id);
		assertThat(response.name()).isEqualTo("Test");
		assertThat(response.items()).hasSize(1);
		assertThat(response.items().get(0).scopeType()).isEqualTo(ResourceType.ORG);
	}

	@Test
	void shouldThrowWhenNotFound() {
		UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.findById(id))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("MembershipTemplate");
	}

	// --- create ---

	@Test
	void shouldCreateSystemTemplate() {
		List<MembershipTemplateItemView> items = List.of(
				new MembershipTemplateItemView(ResourceType.ORG, Map.of("l", "rcud", "m", "r")));
		CreateMembershipTemplateRequest req = new CreateMembershipTemplateRequest(null, "Full Access", null, items);

		when(repository.save(any(MembershipTemplate.class))).thenAnswer(inv -> inv.getArgument(0));

		MembershipTemplateResponse response = service.create(req);

		ArgumentCaptor<MembershipTemplate> captor = ArgumentCaptor.forClass(MembershipTemplate.class);
		verify(repository).save(captor.capture());
		MembershipTemplate captured = captor.getValue();
		assertThat(captured.getOrg()).isNull();
		assertThat(captured.getName()).isEqualTo("Full Access");
		assertThat(captured.getItems()).hasSize(1);
		assertThat(captured.getItems().get(0).getScopeType()).isEqualTo(ResourceType.ORG);
		assertThat(response.name()).isEqualTo("Full Access");
	}

	@Test
	void shouldCreateOrgScopedTemplate() {
		UUID orgId = UUID.randomUUID();
		Organization org = Organization.builder().id(orgId).name("Acme").build();
		List<MembershipTemplateItemView> items = List.of(
				new MembershipTemplateItemView(ResourceType.PROPERTY, Map.of("l", "cru")));
		CreateMembershipTemplateRequest req = new CreateMembershipTemplateRequest(null, "Prop Manager", orgId, items);

		when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
		when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		MembershipTemplateResponse response = service.create(req);

		ArgumentCaptor<MembershipTemplate> captor = ArgumentCaptor.forClass(MembershipTemplate.class);
		verify(repository).save(captor.capture());
		assertThat(captor.getValue().getOrg().getId()).isEqualTo(orgId);
		assertThat(response.orgId()).isEqualTo(orgId);
	}

	@Test
	void shouldThrowWhenCreatingWithInvalidPermissions() {
		List<MembershipTemplateItemView> items = List.of(
				new MembershipTemplateItemView(ResourceType.ORG, Map.of("x", "r"))); // unknown domain
		CreateMembershipTemplateRequest req = new CreateMembershipTemplateRequest(null, "Bad", null, items);

		assertThatThrownBy(() -> service.create(req))
				.isInstanceOf(InvalidPermissionStringException.class);

		verify(repository, never()).save(any());
	}

	@Test
	void create_systemTemplate_deniesNonAdmin() {
		setUserContext();
		List<MembershipTemplateItemView> items = List.of(
				new MembershipTemplateItemView(ResourceType.ORG, Map.of("l", "r")));
		CreateMembershipTemplateRequest req = new CreateMembershipTemplateRequest(null, "System", null, items);

		assertThatThrownBy(() -> service.create(req))
				.isInstanceOf(AccessDeniedException.class);

		verify(repository, never()).save(any());
	}

	// --- update ---

	@Test
	void shouldUpdateTemplateNameAndItems() {
		UUID id = UUID.randomUUID();
		MembershipTemplate existing = template(id, 1, null, "Old", orgItem(Map.of("l", "r")));
		List<MembershipTemplateItemView> newItems = List.of(
				new MembershipTemplateItemView(ResourceType.ORG, Map.of("l", "rcud")));
		UpdateMembershipTemplateRequest req = new UpdateMembershipTemplateRequest("New Name", newItems, 1);

		when(repository.findById(id)).thenReturn(Optional.of(existing));
		when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(membershipRepository.findByMembershipTemplateIdAndUserIsNotNull(id)).thenReturn(List.of());

		MembershipTemplateResponse response = service.update(id, req);

		assertThat(response.name()).isEqualTo("New Name");
		assertThat(response.items()).hasSize(1);
		assertThat(response.items().get(0).permissions()).isEqualTo(Map.of("l", "rcud"));
	}

	@Test
	void update_evictsCacheForLinkedMemberships() {
		UUID id = UUID.randomUUID();
		MembershipTemplate existing = template(id, 0, null, "T", orgItem(Map.of("l", "r")));

		com.akandiah.propmanager.features.user.domain.User user = TestDataFactory.user().build();
		Membership linkedMembership = Membership.builder()
				.id(UUID.randomUUID()).user(user).build();

		List<MembershipTemplateItemView> newItems = List.of(
				new MembershipTemplateItemView(ResourceType.ORG, Map.of("l", "rcud")));
		UpdateMembershipTemplateRequest req = new UpdateMembershipTemplateRequest(null, newItems, 0);

		when(repository.findById(id)).thenReturn(Optional.of(existing));
		when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(membershipRepository.findByMembershipTemplateIdAndUserIsNotNull(id))
				.thenReturn(List.of(linkedMembership));

		service.update(id, req);

		verify(eventPublisher).publishEvent(any(
				com.akandiah.propmanager.features.auth.domain.PermissionsChangedEvent.class));
	}

	@Test
	void shouldThrowOnVersionMismatch() {
		UUID id = UUID.randomUUID();
		MembershipTemplate existing = template(id, 2, null, "T", orgItem(Map.of("l", "r")));
		UpdateMembershipTemplateRequest req = new UpdateMembershipTemplateRequest("T", null, 1);

		when(repository.findById(id)).thenReturn(Optional.of(existing));

		assertThatThrownBy(() -> service.update(id, req))
				.isInstanceOf(OptimisticLockException.class);

		verify(repository, never()).save(any());
	}

	@Test
	void update_orgTemplate_allowsOrgAdmin() {
		setUserContext();
		UUID orgId = UUID.randomUUID();
		UUID id = UUID.randomUUID();
		MembershipTemplate existing = template(id, 0, orgId, "T", orgItem(Map.of("l", "r")));

		when(repository.findById(id)).thenReturn(Optional.of(existing));
		when(permissionGuard.hasAccess(Actions.UPDATE, "o", ResourceType.ORG, orgId, orgId)).thenReturn(true);
		when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		MembershipTemplateResponse response = service.update(id,
				new UpdateMembershipTemplateRequest("New", null, 0));

		assertThat(response.name()).isEqualTo("New");
	}

	@Test
	void update_orgTemplate_deniesRegularMember() {
		setUserContext();
		UUID orgId = UUID.randomUUID();
		UUID id = UUID.randomUUID();
		MembershipTemplate existing = template(id, 0, orgId, "T", orgItem(Map.of("l", "r")));

		when(repository.findById(id)).thenReturn(Optional.of(existing));
		when(permissionGuard.hasAccess(Actions.UPDATE, "o", ResourceType.ORG, orgId, orgId)).thenReturn(false);

		assertThatThrownBy(() -> service.update(id, new UpdateMembershipTemplateRequest("T", null, 0)))
				.isInstanceOf(AccessDeniedException.class);

		verify(repository, never()).save(any());
	}

	// --- delete ---

	@Test
	void shouldDeleteById() {
		UUID id = UUID.randomUUID();
		MembershipTemplate t = template(id, 0, null, "T", orgItem(Map.of("l", "r")));
		when(repository.findById(id)).thenReturn(Optional.of(t));
		when(membershipRepository.findByMembershipTemplateIdAndUserIsNotNull(id)).thenReturn(List.of());

		service.deleteById(id);

		verify(repository).delete(t);
	}

	@Test
	void shouldThrowWhenDeletingNonExistent() {
		UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.deleteById(id))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("MembershipTemplate");

		verify(repository, never()).delete(any());
	}

	@Test
	void delete_systemTemplate_deniesNonAdmin() {
		setUserContext();
		UUID id = UUID.randomUUID();
		MembershipTemplate t = template(id, 0, null, "T", orgItem(Map.of("l", "r")));
		when(repository.findById(id)).thenReturn(Optional.of(t));

		assertThatThrownBy(() -> service.deleteById(id))
				.isInstanceOf(AccessDeniedException.class);

		verify(repository, never()).delete(any());
	}

	// --- helpers ---

	private static MembershipTemplate template(UUID id, int version, UUID orgId, String name, MembershipTemplateItem... items) {
		Organization org = orgId != null
				? Organization.builder().id(orgId).name("Org").build()
				: null;
		return MembershipTemplate.builder()
				.id(id)
				.org(org)
				.name(name)
				.items(new ArrayList<>(List.of(items)))
				.version(version)
				.createdAt(Instant.now())
				.updatedAt(Instant.now())
				.build();
	}

	private static MembershipTemplateItem orgItem(Map<String, String> permissions) {
		return MembershipTemplateItem.builder()
				.scopeType(ResourceType.ORG)
				.permissions(permissions)
				.build();
	}

	private static MembershipTemplateItem propItem(Map<String, String> permissions) {
		return MembershipTemplateItem.builder()
				.scopeType(ResourceType.PROPERTY)
				.permissions(permissions)
				.build();
	}
}
