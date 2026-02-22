package com.akandiah.propmanager.features.organization.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import com.akandiah.propmanager.features.organization.api.dto.CreateMemberScopeRequest;
import com.akandiah.propmanager.features.organization.domain.MemberScope;
import com.akandiah.propmanager.features.organization.domain.MemberScopeRepository;
import com.akandiah.propmanager.features.organization.domain.Membership;
import com.akandiah.propmanager.features.organization.domain.MembershipRepository;
import com.akandiah.propmanager.features.organization.domain.Organization;
import com.akandiah.propmanager.features.organization.domain.Role;
import com.akandiah.propmanager.features.organization.domain.ScopeType;
import com.akandiah.propmanager.features.permission.domain.PermissionTemplate;
import com.akandiah.propmanager.features.user.domain.User;
import com.akandiah.propmanager.features.permission.domain.PermissionTemplateRepository;
import com.akandiah.propmanager.features.prop.domain.PropRepository;
import com.akandiah.propmanager.features.unit.domain.UnitRepository;

@ExtendWith(MockitoExtension.class)
class MemberScopeServiceTest {

	@Mock
	private MemberScopeRepository memberScopeRepository;
	@Mock
	private MembershipRepository membershipRepository;
	@Mock
	private PermissionTemplateRepository permissionTemplateRepository;
	@Mock
	private PropRepository propRepository;
	@Mock
	private UnitRepository unitRepository;

	private MemberScopeService service;

	@BeforeEach
	void setUp() {
		service = new MemberScopeService(
				memberScopeRepository,
				membershipRepository,
				permissionTemplateRepository,
				propRepository,
				unitRepository);
	}

	@Test
	void create_setsPermissionsWhenProvided() {
		UUID membershipId = UUID.randomUUID();
		UUID scopeId = UUID.randomUUID();
		Membership membership = membership(membershipId);
		Map<String, String> perms = Map.of("l", "crud");
		CreateMemberScopeRequest req = new CreateMemberScopeRequest(ScopeType.PROPERTY, scopeId, perms, null);

		when(membershipRepository.findById(membershipId)).thenReturn(Optional.of(membership));
		when(propRepository.existsByIdAndOrganization_Id(scopeId, membership.getOrganization().getId())).thenReturn(true);
		when(memberScopeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		var result = service.create(membershipId, req);

		ArgumentCaptor<MemberScope> captor = ArgumentCaptor.forClass(MemberScope.class);
		verify(memberScopeRepository).save(captor.capture());
		assertThat(captor.getValue().getPermissions()).isEqualTo(perms);
		assertThat(result.permissions()).isEqualTo(perms);
	}

	@Test
	void create_copiesPermissionsFromTemplateWhenTemplateIdProvided() {
		UUID membershipId = UUID.randomUUID();
		UUID scopeId = UUID.randomUUID();
		UUID templateId = UUID.randomUUID();
		Membership membership = membership(membershipId);
		UUID orgId = membership.getOrganization().getId();
		Map<String, String> templatePerms = Map.of("m", "ru", "f", "r");
		PermissionTemplate template = template(null, templatePerms);
		template.setId(templateId);
		CreateMemberScopeRequest req = new CreateMemberScopeRequest(ScopeType.PROPERTY, scopeId, null, templateId);

		when(membershipRepository.findById(membershipId)).thenReturn(Optional.of(membership));
		when(permissionTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));
		when(propRepository.existsByIdAndOrganization_Id(scopeId, orgId)).thenReturn(true);
		when(memberScopeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		var result = service.create(membershipId, req);

		ArgumentCaptor<MemberScope> captor = ArgumentCaptor.forClass(MemberScope.class);
		verify(memberScopeRepository).save(captor.capture());
		assertThat(captor.getValue().getPermissions()).isEqualTo(templatePerms);
		assertThat(result.permissions()).isEqualTo(templatePerms);
	}

	@Test
	void create_usesEmptyPermissionsWhenNoPermissionsOrTemplate() {
		UUID membershipId = UUID.randomUUID();
		UUID scopeId = UUID.randomUUID();
		Membership membership = membership(membershipId);
		CreateMemberScopeRequest req = new CreateMemberScopeRequest(ScopeType.PROPERTY, scopeId, null, null);

		when(membershipRepository.findById(membershipId)).thenReturn(Optional.of(membership));
		when(propRepository.existsByIdAndOrganization_Id(scopeId, membership.getOrganization().getId())).thenReturn(true);
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
		Membership membership = membership(membershipId);
		Map<String, String> invalidPerms = Map.of("l", "x"); // invalid letter
		CreateMemberScopeRequest req = new CreateMemberScopeRequest(ScopeType.PROPERTY, scopeId, invalidPerms, null);

		when(membershipRepository.findById(membershipId)).thenReturn(Optional.of(membership));
		when(propRepository.existsByIdAndOrganization_Id(scopeId, membership.getOrganization().getId())).thenReturn(true);

		assertThatThrownBy(() -> service.create(membershipId, req))
				.isInstanceOf(InvalidPermissionStringException.class);
	}

	private static Organization org(UUID id) {
		return Organization.builder()
				.id(id)
				.name("Test Org")
				.version(0)
				.createdAt(Instant.now())
				.updatedAt(Instant.now())
				.build();
	}

	private static Membership membership(UUID id) {
		UUID orgId = UUID.randomUUID();
		User user = User.builder()
				.id(UUID.randomUUID())
				.name("User")
				.email("u@example.com")
				.phoneNumber("+15551234567")
				.build();
		return Membership.builder()
				.id(id)
				.user(user)
				.organization(org(orgId))
				.role(Role.ADMIN)
				.version(0)
				.createdAt(Instant.now())
				.updatedAt(Instant.now())
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
}
