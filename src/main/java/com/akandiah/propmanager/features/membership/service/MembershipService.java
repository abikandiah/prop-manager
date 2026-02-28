package com.akandiah.propmanager.features.membership.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.features.auth.domain.PermissionsChangedEvent;
import com.akandiah.propmanager.features.invite.domain.Invite;
import com.akandiah.propmanager.features.invite.domain.InviteAcceptedEvent;
import com.akandiah.propmanager.features.invite.domain.InviteRepository;
import com.akandiah.propmanager.features.invite.domain.InviteStatus;
import com.akandiah.propmanager.features.invite.domain.TargetType;
import com.akandiah.propmanager.features.invite.service.InviteService;
import com.akandiah.propmanager.features.membership.api.dto.CreateMembershipRequest;
import com.akandiah.propmanager.features.membership.api.dto.CreatePolicyAssignmentRequest;
import com.akandiah.propmanager.features.membership.api.dto.MembershipResponse;
import com.akandiah.propmanager.features.membership.domain.Membership;
import com.akandiah.propmanager.features.membership.domain.MembershipRepository;
import com.akandiah.propmanager.features.membership.domain.PolicyAssignmentRepository;
import com.akandiah.propmanager.features.organization.domain.Organization;
import com.akandiah.propmanager.features.organization.domain.OrganizationRepository;
import com.akandiah.propmanager.features.user.domain.User;
import com.akandiah.propmanager.features.user.domain.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MembershipService {

	private static final String ATTR_ORG_NAME = "organizationName";
	private static final String ATTR_PREVIEW = "preview";

	private final MembershipRepository membershipRepository;
	private final PolicyAssignmentRepository policyAssignmentRepository;
	private final PolicyAssignmentService policyAssignmentService;
	private final OrganizationRepository organizationRepository;
	private final UserRepository userRepository;
	private final InviteService inviteService;
	private final InviteRepository inviteRepository;
	private final ApplicationEventPublisher eventPublisher;

	public List<MembershipResponse> findByOrganizationId(UUID organizationId) {
		return membershipRepository.findByOrganizationIdWithUserAndOrg(organizationId).stream()
				.map(MembershipResponse::from)
				.toList();
	}

	public List<MembershipResponse> findByUserId(UUID userId) {
		return membershipRepository.findByUserIdWithUserAndOrg(userId).stream()
				.map(MembershipResponse::from)
				.toList();
	}

	public MembershipResponse findById(UUID id) {
		Membership m = membershipRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Membership", id));
		return MembershipResponse.from(m);
	}

	@Transactional
	public MembershipResponse create(UUID organizationId, CreateMembershipRequest request) {
		MembershipResponse membership = doCreate(organizationId, request.userId(), request.id());
		eventPublisher.publishEvent(new PermissionsChangedEvent(Set.of(request.userId())));
		return membership;
	}

	/**
	 * Orchestrates the invitation of a new member.
	 * 1. Creates a pending Membership (user=null).
	 * 2. Creates PolicyAssignment rows from the supplied assignments list.
	 * 3. Creates and sends an Invite pointing to the Membership.
	 */
	@Transactional
	public MembershipResponse inviteMember(UUID organizationId, String email,
			List<CreatePolicyAssignmentRequest> assignments, User invitedBy) {

		userRepository.findByEmail(email).ifPresent(user -> {
			if (membershipRepository.existsByUserIdAndOrganizationId(user.getId(), organizationId)) {
				throw new IllegalStateException("User is already a member of this organization");
			}
		});

		if (membershipRepository.existsPendingInviteForEmailInOrg(email, organizationId)) {
			throw new IllegalStateException("A pending invitation already exists for this email in this organization");
		}

		// 1. Create Membership (user=null)
		MembershipResponse membershipRes = doCreate(organizationId, null, null);

		// 2. Create PolicyAssignment rows
		if (assignments != null) {
			for (CreatePolicyAssignmentRequest req : assignments) {
				policyAssignmentService.createWithoutEvent(membershipRes.id(), req);
			}
		}

		// 3. Create and Send Invite
		Organization org = organizationRepository.findById(organizationId)
				.orElseThrow(() -> new ResourceNotFoundException("Organization", organizationId));

		Map<String, Object> preview = new HashMap<>();
		preview.put("organizationName", org.getName());

		Map<String, Object> attributes = new HashMap<>();
		attributes.put(ATTR_ORG_NAME, org.getName());
		attributes.put(ATTR_PREVIEW, preview);

		var inviteRes = inviteService.createAndSendInvite(
				email,
				TargetType.MEMBERSHIP,
				membershipRes.id(),
				attributes,
				invitedBy);

		// 4. Link Invite back to Membership
		Membership m = membershipRepository.findById(membershipRes.id()).orElseThrow();
		Invite invite = inviteRepository.findById(inviteRes.id()).orElseThrow();
		m.setInvite(invite);
		membershipRepository.save(m);

		return MembershipResponse.from(m);
	}

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
			throw new IllegalStateException(
					"Membership " + membershipId + " is already claimed by user " + membership.getUser().getId());
		}

		membership.setUser(claimedBy);
		membershipRepository.save(membership);

		eventPublisher.publishEvent(new PermissionsChangedEvent(Set.of(claimedBy.getId())));

		log.info("Linked membership id={} to user id={} in org id={}",
				membershipId, claimedBy.getId(), membership.getOrganization().getId());
	}

	private MembershipResponse doCreate(UUID organizationId, UUID userId, UUID id) {
		Organization org = organizationRepository.findById(organizationId)
				.orElseThrow(() -> new ResourceNotFoundException("Organization", organizationId));

		User user = null;
		if (userId != null) {
			user = userRepository.findById(userId)
					.orElseThrow(() -> new ResourceNotFoundException("User", userId));
		}

		Membership m = Membership.builder()
				.id(id)
				.user(user)
				.organization(org)
				.build();
		m = membershipRepository.save(m);
		return MembershipResponse.from(m);
	}

	@Transactional
	public void deleteById(UUID id) {
		Membership m = membershipRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Membership", id));
		doDelete(m);
	}

	@Transactional
	public void deleteById(UUID organizationId, UUID membershipId) {
		Membership m = membershipRepository.findByIdAndOrganizationId(membershipId, organizationId)
				.orElseThrow(() -> new ResourceNotFoundException("Membership", membershipId));
		doDelete(m);
	}

	private void doDelete(Membership m) {
		UUID userId = m.getUser() != null ? m.getUser().getId() : null;

		if (m.getInvite() != null && m.getInvite().getStatus() == InviteStatus.PENDING) {
			inviteService.revokeInvite(m.getInvite().getId());
		}

		policyAssignmentRepository.deleteByMembershipId(m.getId());
		membershipRepository.delete(m);

		if (userId != null) {
			eventPublisher.publishEvent(new PermissionsChangedEvent(Set.of(userId)));
		}
	}
}
