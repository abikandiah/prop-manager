package com.akandiah.propmanager.features.membership.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.features.auth.domain.PermissionsChangedEvent;
import com.akandiah.propmanager.features.invite.domain.Invite;
import com.akandiah.propmanager.features.invite.domain.InviteRepository;
import com.akandiah.propmanager.features.invite.domain.InviteStatus;
import com.akandiah.propmanager.features.invite.domain.TargetType;
import com.akandiah.propmanager.features.invite.service.InviteService;
import com.akandiah.propmanager.features.membership.api.dto.CreateMemberScopeRequest;
import com.akandiah.propmanager.features.membership.api.dto.CreateMembershipRequest;
import com.akandiah.propmanager.features.membership.api.dto.MembershipResponse;
import com.akandiah.propmanager.features.membership.domain.MemberScopeRepository;
import com.akandiah.propmanager.features.membership.domain.Membership;
import com.akandiah.propmanager.features.membership.domain.MembershipRepository;
import com.akandiah.propmanager.features.organization.domain.Organization;
import com.akandiah.propmanager.features.organization.domain.OrganizationRepository;
import com.akandiah.propmanager.features.user.domain.User;
import com.akandiah.propmanager.features.user.domain.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MembershipService {

	// Key constants for invite attributes owned by this domain
	private static final String ATTR_ORG_NAME = "organizationName";
	private static final String ATTR_PREVIEW = "preview";

	private final MembershipRepository membershipRepository;
	private final MemberScopeRepository memberScopeRepository;
	private final MemberScopeService memberScopeService;
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
		MembershipResponse membership = doCreate(organizationId, request.userId());
		eventPublisher.publishEvent(new PermissionsChangedEvent(Set.of(request.userId())));
		return membership;
	}

	/**
	 * Creates a membership and immediately grants an initial scope.
	 * Publishes a single {@link PermissionsChangedEvent} after both operations.
	 */
	@Transactional
	public MembershipResponse createWithInitialScope(UUID organizationId, CreateMembershipRequest request,
			CreateMemberScopeRequest initialScope) {
		MembershipResponse membership = doCreate(organizationId, request.userId());
		if (initialScope != null) {
			memberScopeService.createWithoutEvent(membership.id(), initialScope);
		}
		eventPublisher.publishEvent(new PermissionsChangedEvent(Set.of(request.userId())));
		return membership;
	}

	/**
	 * Orchestrates the invitation of a new member.
	 * 1. Creates a pending Membership (user=null).
	 * 2. Adds initial scopes to the membership.
	 * 3. Creates and sends an Invite pointing to the Membership.
	 */
	@Transactional
	public MembershipResponse inviteMember(UUID organizationId, String email, 
			List<CreateMemberScopeRequest> initialScopes, User invitedBy) {
		
		// Check for existing active membership by email if user exists
		userRepository.findByEmail(email).ifPresent(user -> {
			if (membershipRepository.existsByUserIdAndOrganizationId(user.getId(), organizationId)) {
				throw new IllegalStateException("User is already a member of this organization");
			}
		});

		// Check for existing pending invite for this email in this org
		if (membershipRepository.existsPendingInviteForEmailInOrg(email, organizationId)) {
			throw new IllegalStateException("A pending invitation already exists for this email in this organization");
		}

		// 1. Create Membership (user=null)
		MembershipResponse membershipRes = doCreate(organizationId, null);
		
		// 2. Create Scopes
		if (initialScopes != null) {
			for (CreateMemberScopeRequest scopeReq : initialScopes) {
				memberScopeService.createWithoutEvent(membershipRes.id(), scopeReq);
			}
		}

		// 3. Create and Send Invite
		// We use TargetType.MEMBERSHIP and the targetId is the membership UUID
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

	private MembershipResponse doCreate(UUID organizationId, UUID userId) {
		Organization org = organizationRepository.findById(organizationId)
				.orElseThrow(() -> new ResourceNotFoundException("Organization", organizationId));
		
		User user = null;
		if (userId != null) {
			user = userRepository.findById(userId)
					.orElseThrow(() -> new ResourceNotFoundException("User", userId));
		}

		Membership m = Membership.builder()
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

	/**
	 * Deletes a membership, verifying it belongs to the given organization.
	 */
	@Transactional
	public void deleteById(UUID organizationId, UUID membershipId) {
		Membership m = membershipRepository.findByIdAndOrganizationId(membershipId, organizationId)
				.orElseThrow(() -> new ResourceNotFoundException("Membership", membershipId));
		doDelete(m);
	}

	private void doDelete(Membership m) {
		UUID userId = m.getUser() != null ? m.getUser().getId() : null;

		// Revoke pending invite if it exists
		if (m.getInvite() != null && m.getInvite().getStatus() == InviteStatus.PENDING) {
			inviteService.revokeInvite(m.getInvite().getId());
		}

		memberScopeRepository.deleteByMembershipId(m.getId());
		membershipRepository.delete(m);

		if (userId != null) {
			eventPublisher.publishEvent(new PermissionsChangedEvent(Set.of(userId)));
		}
	}
}
