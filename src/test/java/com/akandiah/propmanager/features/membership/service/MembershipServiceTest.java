package com.akandiah.propmanager.features.membership.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.akandiah.propmanager.TestDataFactory;
import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.common.permission.ResourceType;
import com.akandiah.propmanager.features.auth.domain.PermissionsChangedEvent;
import com.akandiah.propmanager.features.invite.api.dto.InviteResponse;
import com.akandiah.propmanager.features.invite.domain.Invite;
import com.akandiah.propmanager.features.invite.domain.InviteRepository;
import com.akandiah.propmanager.features.invite.domain.InviteStatus;
import com.akandiah.propmanager.features.invite.domain.TargetType;
import com.akandiah.propmanager.features.invite.service.InviteService;
import com.akandiah.propmanager.features.membership.api.dto.CreateMembershipRequest;
import com.akandiah.propmanager.features.membership.api.dto.CreatePolicyAssignmentRequest;
import com.akandiah.propmanager.features.membership.domain.Membership;
import com.akandiah.propmanager.features.membership.domain.MembershipRepository;
import com.akandiah.propmanager.features.membership.domain.PolicyAssignmentRepository;
import com.akandiah.propmanager.features.organization.domain.Organization;
import com.akandiah.propmanager.features.organization.domain.OrganizationRepository;
import com.akandiah.propmanager.features.user.domain.User;
import com.akandiah.propmanager.features.user.domain.UserRepository;

import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {

	@Mock
	private MembershipRepository membershipRepository;
	@Mock
	private PolicyAssignmentRepository policyAssignmentRepository;
	@Mock
	private PolicyAssignmentService policyAssignmentService;
	@Mock
	private OrganizationRepository organizationRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private InviteService inviteService;
	@Mock
	private InviteRepository inviteRepository;
	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private MembershipService service;

	// ─── create() ───────────────────────────────────────────────────────

	@Test
	void create_createsMembershipWithUserAndOrg() {
		UUID orgId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		Organization org = TestDataFactory.organization().id(orgId).build();
		User user = TestDataFactory.user().id(userId).build();
		CreateMembershipRequest req = new CreateMembershipRequest(null, userId);

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
		CreateMembershipRequest req = new CreateMembershipRequest(null, userId);

		when(organizationRepository.findById(orgId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.create(orgId, req))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Organization");
	}

	@Test
	void create_throwsWhenUserNotFound() {
		UUID orgId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		Organization org = TestDataFactory.organization().id(orgId).build();
		CreateMembershipRequest req = new CreateMembershipRequest(null, userId);

		when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
		when(userRepository.findById(userId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.create(orgId, req))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("User");
	}

	// ─── inviteMember() ─────────────────────────────────────────────────

	@Test
	void inviteMember_createsSlotAndInvite() {
		UUID orgId = UUID.randomUUID();
		Organization org = TestDataFactory.organization().id(orgId).build();
		User inviter = TestDataFactory.user().build();
		String email = "newbie@example.com";
		UUID inviteId = UUID.randomUUID();
		UUID membershipId = UUID.randomUUID();

		when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
		when(membershipRepository.existsPendingInviteForEmailInOrg(email, orgId)).thenReturn(false);
		when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
		when(membershipRepository.save(any())).thenAnswer(inv -> {
			Membership m = inv.getArgument(0);
			// simulate ID assignment on first save (slot creation)
			if (m.getId() == null) {
				m = Membership.builder()
						.id(membershipId)
						.user(m.getUser())
						.organization(m.getOrganization())
						.invite(m.getInvite())
						.build();
			}
			return m;
		});

		Invite invite = Invite.builder().id(inviteId).build();
		InviteResponse inviteRes = new InviteResponse(
				inviteId, email, TargetType.MEMBERSHIP, membershipId, null,
				inviter.getId(), inviter.getName(), InviteStatus.PENDING,
				null, null, null, null, null, null, true, false);

		when(inviteService.createAndSendInvite(
				eq(email), eq(TargetType.MEMBERSHIP), eq(membershipId),
				anyMap(), eq(inviter)))
				.thenReturn(inviteRes);
		when(membershipRepository.findById(membershipId)).thenReturn(
				Optional.of(Membership.builder()
						.id(membershipId).organization(org).build()));
		when(inviteRepository.findById(inviteId)).thenReturn(Optional.of(invite));

		var result = service.inviteMember(orgId, email, null, inviter);

		assertThat(result).isNotNull();
		assertThat(result.userId()).isNull();
		verify(inviteService).createAndSendInvite(
				eq(email), eq(TargetType.MEMBERSHIP), eq(membershipId),
				anyMap(), eq(inviter));
	}

	@Test
	void inviteMember_createsInitialAssignments() {
		UUID orgId = UUID.randomUUID();
		Organization org = TestDataFactory.organization().id(orgId).build();
		User inviter = TestDataFactory.user().build();
		String email = "newbie@example.com";
		UUID membershipId = UUID.randomUUID();
		UUID inviteId = UUID.randomUUID();

		CreatePolicyAssignmentRequest assignmentReq = new CreatePolicyAssignmentRequest(
				null, ResourceType.ORG, orgId, null, Map.of("l", "rcud"));

		when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
		when(membershipRepository.existsPendingInviteForEmailInOrg(email, orgId)).thenReturn(false);
		when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
		when(membershipRepository.save(any())).thenAnswer(inv -> {
			Membership m = inv.getArgument(0);
			if (m.getId() == null) {
				return Membership.builder()
						.id(membershipId).user(null).organization(org).build();
			}
			return m;
		});

		Invite invite = Invite.builder().id(inviteId).build();
		InviteResponse inviteRes = new InviteResponse(
				inviteId, email, TargetType.MEMBERSHIP, membershipId, null,
				inviter.getId(), inviter.getName(), InviteStatus.PENDING,
				null, null, null, null, null, null, true, false);

		when(inviteService.createAndSendInvite(
				eq(email), eq(TargetType.MEMBERSHIP), eq(membershipId),
				anyMap(), eq(inviter)))
				.thenReturn(inviteRes);
		when(membershipRepository.findById(membershipId)).thenReturn(
				Optional.of(Membership.builder()
						.id(membershipId).organization(org).build()));
		when(inviteRepository.findById(inviteId)).thenReturn(Optional.of(invite));

		service.inviteMember(orgId, email, List.of(assignmentReq), inviter);

		verify(policyAssignmentService).createWithoutEvent(membershipId, assignmentReq);
	}

	@Test
	void inviteMember_throwsWhenUserAlreadyMember() {
		UUID orgId = UUID.randomUUID();
		String email = "existing@example.com";
		User existingUser = TestDataFactory.user().email(email).build();
		User inviter = TestDataFactory.user().name("Inviter").email("inviter@example.com").build();

		when(userRepository.findByEmail(email)).thenReturn(Optional.of(existingUser));
		when(membershipRepository.existsByUserIdAndOrganizationId(existingUser.getId(), orgId))
				.thenReturn(true);

		assertThatThrownBy(() -> service.inviteMember(orgId, email, null, inviter))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("already a member");
	}

	@Test
	void inviteMember_throwsWhenPendingInviteExists() {
		UUID orgId = UUID.randomUUID();
		String email = "pending@example.com";
		User inviter = TestDataFactory.user().build();

		when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
		when(membershipRepository.existsPendingInviteForEmailInOrg(email, orgId)).thenReturn(true);

		assertThatThrownBy(() -> service.inviteMember(orgId, email, null, inviter))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("pending invitation");
	}

	// ─── deleteById() ───────────────────────────────────────────────────

	@Test
	void deleteById_deletesAndPublishesEventForActiveUser() {
		UUID orgId = UUID.randomUUID();
		UUID membershipId = UUID.randomUUID();
		User user = TestDataFactory.user().build();
		Organization org = TestDataFactory.organization().id(orgId).build();
		Membership membership = Membership.builder()
				.id(membershipId).user(user).organization(org).build();

		when(membershipRepository.findByIdAndOrganizationId(membershipId, orgId))
				.thenReturn(Optional.of(membership));

		service.deleteById(orgId, membershipId);

		verify(policyAssignmentRepository).deleteByMembershipId(membershipId);
		verify(membershipRepository).delete(membership);

		ArgumentCaptor<PermissionsChangedEvent> eventCaptor = ArgumentCaptor.forClass(PermissionsChangedEvent.class);
		verify(eventPublisher).publishEvent(eventCaptor.capture());
		assertThat(eventCaptor.getValue().affectedUserIds()).isEqualTo(Set.of(user.getId()));
	}

	@Test
	void deleteById_deletesWithoutEventForPendingInvite() {
		UUID orgId = UUID.randomUUID();
		UUID membershipId = UUID.randomUUID();
		Organization org = TestDataFactory.organization().id(orgId).build();
		Membership membership = Membership.builder()
				.id(membershipId).user(null).organization(org).build();

		when(membershipRepository.findByIdAndOrganizationId(membershipId, orgId))
				.thenReturn(Optional.of(membership));

		service.deleteById(orgId, membershipId);

		verify(policyAssignmentRepository).deleteByMembershipId(membershipId);
		verify(membershipRepository).delete(membership);
		verify(eventPublisher, never()).publishEvent(any(PermissionsChangedEvent.class));
		verify(inviteService, never()).revokeInvite(any());
	}

	@Test
	void deleteById_revokesPendingInvite() {
		UUID orgId = UUID.randomUUID();
		UUID membershipId = UUID.randomUUID();
		UUID inviteId = UUID.randomUUID();
		Organization org = TestDataFactory.organization().id(orgId).build();
		Invite invite = Invite.builder()
				.id(inviteId)
				.status(InviteStatus.PENDING)
				.build();
		Membership membership = Membership.builder()
				.id(membershipId)
				.user(null)
				.organization(org)
				.invite(invite)
				.build();

		when(membershipRepository.findByIdAndOrganizationId(membershipId, orgId))
				.thenReturn(Optional.of(membership));

		service.deleteById(orgId, membershipId);

		verify(inviteService).revokeInvite(inviteId);
		verify(policyAssignmentRepository).deleteByMembershipId(membershipId);
		verify(membershipRepository).delete(membership);
	}

	@Test
	void deleteById_throwsWhenNotFound() {
		UUID orgId = UUID.randomUUID();
		UUID membershipId = UUID.randomUUID();

		when(membershipRepository.findByIdAndOrganizationId(membershipId, orgId))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.deleteById(orgId, membershipId))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Membership");
	}
}
