package com.akandiah.propmanager.features.membership.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.akandiah.propmanager.TestDataFactory;
import com.akandiah.propmanager.features.auth.domain.PermissionsChangedEvent;
import com.akandiah.propmanager.features.invite.domain.Invite;
import com.akandiah.propmanager.features.invite.domain.InviteAcceptedEvent;
import com.akandiah.propmanager.features.invite.domain.TargetType;
import com.akandiah.propmanager.features.membership.domain.Membership;
import com.akandiah.propmanager.features.membership.domain.MembershipRepository;
import com.akandiah.propmanager.features.organization.domain.Organization;
import com.akandiah.propmanager.features.user.domain.User;

@ExtendWith(MockitoExtension.class)
class MemberInviteAcceptedListenerTest {

	@Mock
	private MembershipRepository membershipRepository;
	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private MembershipService service;

	@Test
	void onInviteAccepted_shouldThrowErrorIfMembershipAlreadyClaimed() {
		// Given
		User existingUser = TestDataFactory.user().build();
		User claimedBy = TestDataFactory.user().build();

		Membership membership = Membership.builder()
				.id(UUID.randomUUID())
				.user(existingUser) // ALREADY CLAIMED
				.build();

		Invite invite = Invite.builder()
				.id(UUID.randomUUID())
				.targetType(TargetType.MEMBERSHIP)
				.targetId(membership.getId())
				.build();

		InviteAcceptedEvent event = new InviteAcceptedEvent(invite, claimedBy);

		when(membershipRepository.findById(membership.getId())).thenReturn(Optional.of(membership));

		// When / Then
		assertThatThrownBy(() -> service.onInviteAccepted(event))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("already claimed");

		verify(membershipRepository, never()).save(any());
	}

	@Test
	void onInviteAccepted_shouldLinkUserIfMembershipNotClaimed() {
		// Given
		User claimedBy = TestDataFactory.user().build();
		UUID orgId = UUID.randomUUID();
		Organization org = Organization.builder()
				.id(orgId)
				.build();

		Membership membership = Membership.builder()
				.id(UUID.randomUUID())
				.organization(org)
				.user(null) // NOT CLAIMED
				.build();

		Invite invite = Invite.builder()
				.id(UUID.randomUUID())
				.targetType(TargetType.MEMBERSHIP)
				.targetId(membership.getId())
				.build();

		InviteAcceptedEvent event = new InviteAcceptedEvent(invite, claimedBy);

		when(membershipRepository.findById(membership.getId())).thenReturn(Optional.of(membership));

		// When
		service.onInviteAccepted(event);

		// Then
		verify(membershipRepository).save(membership);
		assertThat(membership.getUser()).isEqualTo(claimedBy);

		verify(eventPublisher).publishEvent(any(PermissionsChangedEvent.class));
	}
}
