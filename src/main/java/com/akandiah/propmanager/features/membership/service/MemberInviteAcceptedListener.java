package com.akandiah.propmanager.features.membership.service;

import java.util.Set;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.features.auth.domain.PermissionsChangedEvent;
import com.akandiah.propmanager.features.invite.domain.InviteAcceptedEvent;
import com.akandiah.propmanager.features.invite.domain.TargetType;
import com.akandiah.propmanager.features.membership.domain.Membership;
import com.akandiah.propmanager.features.membership.domain.MembershipRepository;
import com.akandiah.propmanager.features.user.domain.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles the post-acceptance side effects for membership invites.
 *
 * <p>On accept: finds the pre-created Membership and links the user.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MemberInviteAcceptedListener {

	private final MembershipRepository membershipRepository;
	private final ApplicationEventPublisher eventPublisher;

	@EventListener
	@Transactional
	public void onInviteAccepted(InviteAcceptedEvent event) {
		if (event.invite().getTargetType() != TargetType.MEMBERSHIP) {
			return;
		}

		User claimedBy = event.claimedUser();
		UUID membershipId = event.invite().getTargetId();

		log.info("Processing membership invite acceptance: user={}, membership={}", 
				claimedBy.getId(), membershipId);

		Membership membership = membershipRepository.findById(membershipId)
				.orElseThrow(() -> new ResourceNotFoundException("Membership", membershipId));
		
		if (membership.getUser() != null) {
			log.warn("Membership {} is already claimed by user {}", membershipId, membership.getUser().getId());
			return;
		}

		membership.setUser(claimedBy);
		membershipRepository.save(membership);

		eventPublisher.publishEvent(new PermissionsChangedEvent(Set.of(claimedBy.getId())));

		log.info("Linked membership id={} to user id={} in org id={}",
				membershipId, claimedBy.getId(), membership.getOrganization().getId());
	}
}
