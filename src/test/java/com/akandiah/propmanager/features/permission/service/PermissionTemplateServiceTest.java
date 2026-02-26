package com.akandiah.propmanager.features.permission.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.akandiah.propmanager.common.exception.InvalidPermissionStringException;
import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.common.permission.Actions;
import com.akandiah.propmanager.common.permission.ResourceType;
import com.akandiah.propmanager.features.organization.domain.Organization;
import com.akandiah.propmanager.features.organization.domain.OrganizationRepository;
import com.akandiah.propmanager.features.permission.api.dto.CreatePermissionTemplateRequest;
import com.akandiah.propmanager.features.permission.api.dto.PermissionTemplateResponse;
import com.akandiah.propmanager.features.permission.api.dto.UpdatePermissionTemplateRequest;
import com.akandiah.propmanager.features.permission.domain.PermissionTemplate;
import com.akandiah.propmanager.features.permission.domain.PermissionTemplateRepository;
import com.akandiah.propmanager.security.OrgGuard;
import com.akandiah.propmanager.security.PermissionGuard;

import jakarta.persistence.OptimisticLockException;

@ExtendWith(MockitoExtension.class)
class PermissionTemplateServiceTest {

	@Mock
	private PermissionTemplateRepository repository;

	@Mock
	private OrganizationRepository organizationRepository;

	@Mock
	private OrgGuard orgGuard;

	@Mock
	private PermissionGuard permissionGuard;

	private PermissionTemplateService service;

	@BeforeEach
	void setUp() {
		// Default context: global admin so existing behaviour tests pass unchanged
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken("admin", null,
						List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
		service = new PermissionTemplateService(repository, organizationRepository, orgGuard, permissionGuard);
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

	@Test
	void shouldListByOrg() {
		UUID orgId = UUID.randomUUID();
		PermissionTemplate system = template(null, "System Full", Map.of("l", "crud", "m", "r"));
		PermissionTemplate orgTpl = template(orgId, "Org Template", Map.of("l", "ru"));

		when(repository.findByOrgIsNullOrOrg_IdOrderByNameAsc(orgId))
				.thenReturn(List.of(system, orgTpl));

		List<PermissionTemplateResponse> result = service.listByOrg(orgId);

		assertThat(result).hasSize(2);
		assertThat(result.get(0).name()).isEqualTo("System Full");
		assertThat(result.get(0).orgId()).isNull();
		assertThat(result.get(1).name()).isEqualTo("Org Template");
		assertThat(result.get(1).orgId()).isEqualTo(orgId);
	}

	@Test
	void shouldReturnEmptyListWhenNoTemplates() {
		UUID orgId = UUID.randomUUID();
		when(repository.findByOrgIsNullOrOrg_IdOrderByNameAsc(orgId)).thenReturn(List.of());

		List<PermissionTemplateResponse> result = service.listByOrg(orgId);

		assertThat(result).isEmpty();
	}

	@Test
	void shouldFindById() {
		UUID id = UUID.randomUUID();
		PermissionTemplate t = template(null, "Test", Map.of("l", "r"));
		t.setId(id);
		when(repository.findById(id)).thenReturn(Optional.of(t));

		PermissionTemplateResponse response = service.findById(id);

		assertThat(response.id()).isEqualTo(id);
		assertThat(response.name()).isEqualTo("Test");
		assertThat(response.defaultPermissions()).isEqualTo(Map.of("l", "r"));
	}

	@Test
	void shouldThrowWhenNotFound() {
		UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.findById(id))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("PermissionTemplate")
				.hasMessageContaining(id.toString());
	}

	@Test
	void shouldCreateSystemTemplate() {
		CreatePermissionTemplateRequest req = new CreatePermissionTemplateRequest(
				"Full Access", null, Map.of("l", "crud", "m", "r", "f", "r"));
		PermissionTemplate saved = template(null, req.name(), req.defaultPermissions());
		UUID id = UUID.randomUUID();
		saved.setId(id);

		when(repository.save(any(PermissionTemplate.class))).thenAnswer(inv -> {
			PermissionTemplate p = inv.getArgument(0);
			p.setId(id);
			return p;
		});

		PermissionTemplateResponse response = service.create(req);

		ArgumentCaptor<PermissionTemplate> captor = ArgumentCaptor.forClass(PermissionTemplate.class);
		verify(repository).save(captor.capture());
		PermissionTemplate captured = captor.getValue();
		assertThat(captured.getOrg()).isNull();
		assertThat(captured.getName()).isEqualTo("Full Access");
		assertThat(captured.getDefaultPermissions()).isEqualTo(Map.of("l", "crud", "m", "r", "f", "r"));
		assertThat(response.name()).isEqualTo("Full Access");
	}

	@Test
	void shouldCreateOrgScopedTemplate() {
		UUID orgId = UUID.randomUUID();
		Organization org = Organization.builder().id(orgId).name("Acme").version(0).build();
		CreatePermissionTemplateRequest req = new CreatePermissionTemplateRequest(
				"Org Template", orgId, Map.of("l", "cru"));
		PermissionTemplate saved = template(orgId, req.name(), req.defaultPermissions());
		UUID id = UUID.randomUUID();
		saved.setId(id);

		when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
		when(repository.save(any(PermissionTemplate.class))).thenAnswer(inv -> {
			PermissionTemplate p = inv.getArgument(0);
			p.setId(id);
			return p;
		});

		PermissionTemplateResponse response = service.create(req);

		ArgumentCaptor<PermissionTemplate> captor = ArgumentCaptor.forClass(PermissionTemplate.class);
		verify(repository).save(captor.capture());
		assertThat(captor.getValue().getOrg().getId()).isEqualTo(orgId);
		assertThat(response.orgId()).isEqualTo(orgId);
	}

	@Test
	void shouldThrowWhenCreatingWithInvalidPermissions() {
		CreatePermissionTemplateRequest req = new CreatePermissionTemplateRequest(
				"Bad", null, Map.of("x", "r")); // invalid domain "x"

		assertThatThrownBy(() -> service.create(req))
				.isInstanceOf(InvalidPermissionStringException.class);

		verify(repository, never()).save(any());
	}

	@Test
	void shouldThrowWhenOrgNotFoundOnCreate() {
		UUID orgId = UUID.randomUUID();
		CreatePermissionTemplateRequest req = new CreatePermissionTemplateRequest(
				"Tpl", orgId, Map.of("l", "r"));

		when(organizationRepository.findById(orgId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.create(req))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Organization")
				.hasMessageContaining(orgId.toString());

		verify(repository, never()).save(any());
	}

	@Test
	void shouldUpdateTemplate() {
		UUID id = UUID.randomUUID();
		PermissionTemplate existing = template(null, "Old", Map.of("l", "r"));
		existing.setId(id);
		existing.setVersion(1);
		UpdatePermissionTemplateRequest req = new UpdatePermissionTemplateRequest(
				"New Name", Map.of("l", "crud"), 1);

		when(repository.findById(id)).thenReturn(Optional.of(existing));
		when(repository.save(any(PermissionTemplate.class))).thenAnswer(inv -> inv.getArgument(0));

		PermissionTemplateResponse response = service.update(id, req);

		assertThat(response.name()).isEqualTo("New Name");
		assertThat(response.defaultPermissions()).isEqualTo(Map.of("l", "crud"));
		verify(repository).save(existing);
		assertThat(existing.getName()).isEqualTo("New Name");
		assertThat(existing.getDefaultPermissions()).isEqualTo(Map.of("l", "crud"));
	}

	@Test
	void shouldThrowOnVersionMismatch() {
		UUID id = UUID.randomUUID();
		PermissionTemplate existing = template(null, "T", Map.of("l", "r"));
		existing.setId(id);
		existing.setVersion(2);
		UpdatePermissionTemplateRequest req = new UpdatePermissionTemplateRequest("T", Map.of("l", "r"), 1);

		when(repository.findById(id)).thenReturn(Optional.of(existing));

		assertThatThrownBy(() -> service.update(id, req))
				.isInstanceOf(OptimisticLockException.class);

		verify(repository, never()).save(any());
	}

	@Test
	void shouldThrowWhenUpdatingWithInvalidPermissions() {
		UUID id = UUID.randomUUID();
		PermissionTemplate existing = template(null, "T", Map.of("l", "r"));
		existing.setId(id);
		existing.setVersion(0);
		UpdatePermissionTemplateRequest req = new UpdatePermissionTemplateRequest(
				null, Map.of("l", "x"), 0); // invalid letter "x"

		when(repository.findById(id)).thenReturn(Optional.of(existing));

		assertThatThrownBy(() -> service.update(id, req))
				.isInstanceOf(InvalidPermissionStringException.class);

		verify(repository, never()).save(any());
	}

	@Test
	void shouldDeleteById() {
		UUID id = UUID.randomUUID();
		PermissionTemplate t = template(null, "T", Map.of("l", "r"));
		t.setId(id);
		when(repository.findById(id)).thenReturn(Optional.of(t));

		service.deleteById(id);

		verify(repository).delete(t);
	}

	@Test
	void shouldThrowWhenDeletingNonExistent() {
		UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.deleteById(id))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("PermissionTemplate");

		verify(repository, never()).delete(any());
	}

	// --- Auth tests ---

	@Test
	void findById_systemTemplate_allowsAnyAuthenticatedUser() {
		setUserContext();
		UUID id = UUID.randomUUID();
		PermissionTemplate t = template(null, "System", Map.of("l", "r"));
		t.setId(id);
		when(repository.findById(id)).thenReturn(Optional.of(t));

		PermissionTemplateResponse response = service.findById(id);

		assertThat(response.id()).isEqualTo(id);
	}

	@Test
	void findById_orgTemplate_deniesNonMember() {
		setUserContext();
		UUID orgId = UUID.randomUUID();
		UUID id = UUID.randomUUID();
		PermissionTemplate t = template(orgId, "Org", Map.of("l", "r"));
		t.setId(id);
		when(repository.findById(id)).thenReturn(Optional.of(t));
		when(orgGuard.isMember(any(), any())).thenReturn(false);

		assertThatThrownBy(() -> service.findById(id))
				.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void findById_orgTemplate_allowsOrgMember() {
		setUserContext();
		UUID orgId = UUID.randomUUID();
		UUID id = UUID.randomUUID();
		PermissionTemplate t = template(orgId, "Org", Map.of("l", "r"));
		t.setId(id);
		when(repository.findById(id)).thenReturn(Optional.of(t));
		when(orgGuard.isMember(any(), any())).thenReturn(true);

		PermissionTemplateResponse response = service.findById(id);

		assertThat(response.orgId()).isEqualTo(orgId);
	}

	@Test
	void create_systemTemplate_deniesNonAdmin() {
		setUserContext();
		CreatePermissionTemplateRequest req = new CreatePermissionTemplateRequest(
				"System", null, Map.of("l", "r"));

		assertThatThrownBy(() -> service.create(req))
				.isInstanceOf(AccessDeniedException.class);

		verify(repository, never()).save(any());
	}

	@Test
	void update_systemTemplate_deniesNonAdmin() {
		setUserContext();
		UUID id = UUID.randomUUID();
		PermissionTemplate existing = template(null, "T", Map.of("l", "r"));
		existing.setId(id);
		existing.setVersion(0);
		when(repository.findById(id)).thenReturn(Optional.of(existing));

		assertThatThrownBy(() -> service.update(id, new UpdatePermissionTemplateRequest("T", null, 0)))
				.isInstanceOf(AccessDeniedException.class);

		verify(repository, never()).save(any());
	}

	@Test
	void update_orgTemplate_allowsOrgAdmin() {
		setUserContext();
		UUID orgId = UUID.randomUUID();
		UUID id = UUID.randomUUID();
		PermissionTemplate existing = template(orgId, "T", Map.of("l", "r"));
		existing.setId(id);
		existing.setVersion(0);
		when(repository.findById(id)).thenReturn(Optional.of(existing));
		when(permissionGuard.hasAccess(Actions.UPDATE, "o", ResourceType.ORG, orgId, orgId)).thenReturn(true);
		when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		PermissionTemplateResponse response = service.update(id, new UpdatePermissionTemplateRequest("New", null, 0));

		assertThat(response.name()).isEqualTo("New");
	}

	@Test
	void update_orgTemplate_deniesRegularMember() {
		setUserContext();
		UUID orgId = UUID.randomUUID();
		UUID id = UUID.randomUUID();
		PermissionTemplate existing = template(orgId, "T", Map.of("l", "r"));
		existing.setId(id);
		existing.setVersion(0);
		when(repository.findById(id)).thenReturn(Optional.of(existing));
		when(permissionGuard.hasAccess(Actions.UPDATE, "o", ResourceType.ORG, orgId, orgId)).thenReturn(false);

		assertThatThrownBy(() -> service.update(id, new UpdatePermissionTemplateRequest("T", null, 0)))
				.isInstanceOf(AccessDeniedException.class);

		verify(repository, never()).save(any());
	}

	@Test
	void delete_systemTemplate_deniesNonAdmin() {
		setUserContext();
		UUID id = UUID.randomUUID();
		PermissionTemplate t = template(null, "T", Map.of("l", "r"));
		t.setId(id);
		when(repository.findById(id)).thenReturn(Optional.of(t));

		assertThatThrownBy(() -> service.deleteById(id))
				.isInstanceOf(AccessDeniedException.class);

		verify(repository, never()).delete(any());
	}

	@Test
	void delete_orgTemplate_deniesRegularMember() {
		setUserContext();
		UUID orgId = UUID.randomUUID();
		UUID id = UUID.randomUUID();
		PermissionTemplate t = template(orgId, "T", Map.of("l", "r"));
		t.setId(id);
		when(repository.findById(id)).thenReturn(Optional.of(t));
		when(permissionGuard.hasAccess(Actions.DELETE, "o", ResourceType.ORG, orgId, orgId)).thenReturn(false);

		assertThatThrownBy(() -> service.deleteById(id))
				.isInstanceOf(AccessDeniedException.class);

		verify(repository, never()).delete(any());
	}

	private static PermissionTemplate template(UUID orgId, String name, Map<String, String> perms) {
		Organization org = orgId != null
				? Organization.builder().id(orgId).name("Org").version(0).build()
				: null;
		PermissionTemplate t = PermissionTemplate.builder()
				.org(org)
				.name(name)
				.defaultPermissions(perms)
				.version(0)
				.createdAt(Instant.now())
				.updatedAt(Instant.now())
				.build();
		return t;
	}
}
