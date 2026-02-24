package com.akandiah.propmanager.features.organization.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.features.organization.api.dto.CreateMembershipRequest;
import com.akandiah.propmanager.features.organization.domain.MemberScopeRepository;
import com.akandiah.propmanager.features.organization.domain.Membership;
import com.akandiah.propmanager.features.organization.domain.MembershipRepository;
import com.akandiah.propmanager.features.organization.domain.Organization;
import com.akandiah.propmanager.features.organization.domain.OrganizationRepository;
import com.akandiah.propmanager.features.user.domain.User;
import com.akandiah.propmanager.features.user.domain.UserRepository;

@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {

	@Mock
	private MembershipRepository membershipRepository;
	@Mock
	private MemberScopeRepository memberScopeRepository;
	@Mock
	private MemberScopeService memberScopeService;
	@Mock
	private OrganizationRepository organizationRepository;
	@Mock
	private UserRepository userRepository;

	private MembershipService service;

	@BeforeEach
	void setUp() {
		service = new MembershipService(
				membershipRepository,
				memberScopeRepository,
				memberScopeService,
				organizationRepository,
				userRepository);
	}

	@Test
	void create_createsMembershipWithUserAndOrg() {
		UUID orgId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		Organization org = org(orgId);
		User user = user(userId);
		CreateMembershipRequest req = new CreateMembershipRequest(userId);

		when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
		when(userRepository.findById(userId)).thenReturn(Optional.of(user));
		when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		var result = service.create(orgId, req);

		ArgumentCaptor<Membership> captor = ArgumentCaptor.forClass(Membership.class);
		verify(membershipRepository).save(captor.capture());
		assertThat(captor.getValue().getUser()).isEqualTo(user);
		assertThat(captor.getValue().getOrganization()).isEqualTo(org);
		assertThat(result.userId()).isEqualTo(userId);
		assertThat(result.organizationId()).isEqualTo(orgId);
	}

	@Test
	void create_throwsWhenOrgNotFound() {
		UUID orgId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		CreateMembershipRequest req = new CreateMembershipRequest(userId);

		when(organizationRepository.findById(orgId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.create(orgId, req))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Organization");
	}

	@Test
	void create_throwsWhenUserNotFound() {
		UUID orgId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		Organization org = org(orgId);
		CreateMembershipRequest req = new CreateMembershipRequest(userId);

		when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
		when(userRepository.findById(userId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.create(orgId, req))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("User");
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

	private static User user(UUID id) {
		return User.builder()
				.id(id)
				.name("Test User")
				.email("test@example.com")
				.phoneNumber("+15551234567")
				.build();
	}

}
