package com.akandiah.propmanager.features.organization.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.akandiah.propmanager.common.exception.InvalidPermissionStringException;
import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.features.organization.api.dto.CreateMembershipRequest;
import com.akandiah.propmanager.features.organization.api.dto.UpdateMembershipRequest;
import com.akandiah.propmanager.features.organization.domain.MemberScopeRepository;
import com.akandiah.propmanager.features.organization.domain.Membership;
import com.akandiah.propmanager.features.organization.domain.MembershipRepository;
import com.akandiah.propmanager.features.organization.domain.Organization;
import com.akandiah.propmanager.features.organization.domain.Role;
import com.akandiah.propmanager.features.organization.domain.OrganizationRepository;
import com.akandiah.propmanager.features.permission.domain.PermissionTemplate;
import com.akandiah.propmanager.features.permission.domain.PermissionTemplateRepository;
import com.akandiah.propmanager.features.user.domain.User;
import com.akandiah.propmanager.features.user.domain.UserRepository;

@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {

	@Mock
	private MembershipRepository membershipRepository;
	@Mock
	private MemberScopeRepository memberScopeRepository;
	@Mock
	private OrganizationRepository organizationRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private PermissionTemplateRepository permissionTemplateRepository;

	private MembershipService service;

	@BeforeEach
	void setUp() {
		service = new MembershipService(
				membershipRepository,
				memberScopeRepository,
				organizationRepository,
				userRepository,
				permissionTemplateRepository);
	}

	@Test
	void create_setsPermissionsWhenProvided() {
		UUID orgId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		Organization org = org(orgId);
		User user = user(userId);
		Map<String, String> perms = Map.of("l", "cru", "m", "r");
		CreateMembershipRequest req = new CreateMembershipRequest(userId, Role.MANAGER, perms, null);

		when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
		when(userRepository.findById(userId)).thenReturn(Optional.of(user));
		when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		var result = service.create(orgId, req);

		ArgumentCaptor<Membership> captor = ArgumentCaptor.forClass(Membership.class);
		verify(membershipRepository).save(captor.capture());
		assertThat(captor.getValue().getPermissions()).isEqualTo(perms);
		assertThat(result.permissions()).isEqualTo(perms);
	}

	@Test
	void create_copiesPermissionsFromTemplateWhenTemplateIdProvided() {
		UUID orgId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID templateId = UUID.randomUUID();
		Organization org = org(orgId);
		User user = user(userId);
		Map<String, String> templatePerms = Map.of("l", "crud", "f", "r");
		PermissionTemplate template = template(null, templatePerms);
		template.setId(templateId);
		CreateMembershipRequest req = new CreateMembershipRequest(userId, Role.ADMIN, null, templateId);

		when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
		when(userRepository.findById(userId)).thenReturn(Optional.of(user));
		when(permissionTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));
		when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		var result = service.create(orgId, req);

		ArgumentCaptor<Membership> captor = ArgumentCaptor.forClass(Membership.class);
		verify(membershipRepository).save(captor.capture());
		assertThat(captor.getValue().getPermissions()).isEqualTo(templatePerms);
		assertThat(result.permissions()).isEqualTo(templatePerms);
	}

	@Test
	void create_throwsWhenTemplateNotFound() {
		UUID orgId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID templateId = UUID.randomUUID();
		Organization org = org(orgId);
		User user = user(userId);
		CreateMembershipRequest req = new CreateMembershipRequest(userId, Role.ADMIN, null, templateId);

		when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
		when(userRepository.findById(userId)).thenReturn(Optional.of(user));
		when(permissionTemplateRepository.findById(templateId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.create(orgId, req))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("PermissionTemplate");
	}

	@Test
	void create_throwsWhenTemplateBelongsToDifferentOrg() {
		UUID orgId = UUID.randomUUID();
		UUID otherOrgId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID templateId = UUID.randomUUID();
		Organization org = org(orgId);
		User user = user(userId);
		PermissionTemplate template = template(otherOrgId, Map.of("l", "r"));
		template.setId(templateId);
		CreateMembershipRequest req = new CreateMembershipRequest(userId, Role.ADMIN, null, templateId);

		when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
		when(userRepository.findById(userId)).thenReturn(Optional.of(user));
		when(permissionTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));

		assertThatThrownBy(() -> service.create(orgId, req))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("does not belong to this organization");
	}

	@Test
	void create_throwsWhenPermissionsInvalid() {
		UUID orgId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		Organization org = org(orgId);
		User user = user(userId);
		Map<String, String> invalidPerms = Map.of("x", "r"); // unknown domain
		CreateMembershipRequest req = new CreateMembershipRequest(userId, Role.ADMIN, invalidPerms, null);

		when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
		when(userRepository.findById(userId)).thenReturn(Optional.of(user));

		assertThatThrownBy(() -> service.create(orgId, req))
				.isInstanceOf(InvalidPermissionStringException.class);
	}

	@Test
	void update_setsPermissionsWhenProvided() {
		UUID id = UUID.randomUUID();
		Membership m = membership(id, 1);
		Map<String, String> perms = Map.of("m", "cru");
		UpdateMembershipRequest req = new UpdateMembershipRequest(Role.MANAGER, perms, 1);

		when(membershipRepository.findById(id)).thenReturn(Optional.of(m));
		when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		var result = service.update(id, req);

		verify(membershipRepository).save(m);
		assertThat(m.getPermissions()).isEqualTo(perms);
		assertThat(result.permissions()).isEqualTo(perms);
	}

	@Test
	void update_throwsWhenPermissionsInvalid() {
		UUID id = UUID.randomUUID();
		Membership m = membership(id, 1);
		Map<String, String> invalidPerms = Map.of("l", "z"); // invalid letter
		UpdateMembershipRequest req = new UpdateMembershipRequest(Role.ADMIN, invalidPerms, 1);

		when(membershipRepository.findById(id)).thenReturn(Optional.of(m));

		assertThatThrownBy(() -> service.update(id, req))
				.isInstanceOf(InvalidPermissionStringException.class);
		verify(membershipRepository, never()).save(any());
	}

	private static Organization org(UUID id) {
		Organization o = Organization.builder()
				.id(id)
				.name("Test Org")
				.version(0)
				.createdAt(Instant.now())
				.updatedAt(Instant.now())
				.build();
		return o;
	}

	private static User user(UUID id) {
		return User.builder()
				.id(id)
				.name("Test User")
				.email("test@example.com")
				.phoneNumber("+15551234567")
				.build();
	}

	private static PermissionTemplate template(UUID orgId, Map<String, String> perms) {
		Organization org = orgId != null ? org(orgId) : null;
		return PermissionTemplate.builder()
				.org(org)
				.name("Template")
				.defaultPermissions(perms)
				.version(0)
				.createdAt(Instant.now())
				.updatedAt(Instant.now())
				.build();
	}

	private static Membership membership(UUID id, int version) {
		Membership m = Membership.builder()
				.id(id)
				.user(user(UUID.randomUUID()))
				.organization(org(UUID.randomUUID()))
				.role(Role.ADMIN)
				.version(version)
				.createdAt(Instant.now())
				.updatedAt(Instant.now())
				.build();
		return m;
	}
}
