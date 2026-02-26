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
import com.akandiah.propmanager.features.membership.api.dto.ApplyTemplateRequest;
import com.akandiah.propmanager.features.membership.api.dto.CreateMemberScopeRequest;
import com.akandiah.propmanager.features.membership.api.dto.CreateMembershipRequest;
import com.akandiah.propmanager.features.membership.api.dto.MembershipResponse;
import com.akandiah.propmanager.features.membership.domain.MemberScopeRepository;
import com.akandiah.propmanager.features.membership.domain.Membership;
import com.akandiah.propmanager.features.membership.domain.MembershipRepository;
import com.akandiah.propmanager.features.membership.domain.MembershipTemplate;
import com.akandiah.propmanager.features.membership.domain.MembershipTemplateRepository;
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

	// Key constants for invite attributes owned by this domain
	private static final String ATTR_ORG_NAME = "organizationName";
	private static final String ATTR_PREVIEW = "preview";
	private static final String ATTR_TEMPLATE_ID = "templateId";

	private final MembershipRepository membershipRepository;
	private final MemberScopeRepository memberScopeRepository;
	private final MemberScopeService memberScopeService;
	private final MembershipTemplateRepository membershipTemplateRepository;
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
		MembershipResponse membership = doCreate(organizationId, request.userId(), null);
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
		MembershipResponse membership = doCreate(organizationId, request.userId(), null);
		if (initialScope != null) {
			memberScopeService.createWithoutEvent(membership.id(), initialScope);
		}
		eventPublisher.publishEvent(new PermissionsChangedEvent(Set.of(request.userId())));
		return membership;
	}

	/**
	 * Orchestrates the invitation of a new member.
	 * 1. Creates a pending Membership (user=null), optionally linked to a template.
	 * 2. Adds initial scopes to the membership.
	 * 3. Creates and sends an Invite pointing to the Membership.
	 *
	 * <p>If {@code templateId} is provided, the Membership is linked to the given
	 * {@link MembershipTemplate} at creation time. Template permissions are then
	 * resolved live at JWT hydration — no extra scope rows are needed for ORG-level
	 * template items. For PROPERTY/UNIT-level items, callers should include the
	 * corresponding binding rows in {@code initialScopes} (empty {@code permissions}
	 * map is sufficient to activate the template at that resource).
	 */
	@Transactional
	public MembershipResponse inviteMember(UUID organizationId, String email,
			UUID templateId, List<CreateMemberScopeRequest> initialScopes, User invitedBy) {

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

		// Resolve template if provided
		MembershipTemplate template = null;
		if (templateId != null) {
			template = membershipTemplateRepository.findById(templateId)
					.orElseThrow(() -> new ResourceNotFoundException("MembershipTemplate", templateId));
		}

		// 1. Create Membership (user=null), optionally linked to template
		MembershipResponse membershipRes = doCreate(organizationId, null, template);

		// 2. Create Scopes (binding rows + explicit custom permissions)
		if (initialScopes != null) {
			for (CreateMemberScopeRequest scopeReq : initialScopes) {
				memberScopeService.createWithoutEvent(membershipRes.id(), scopeReq);
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
		if (templateId != null) {
			attributes.put(ATTR_TEMPLATE_ID, templateId.toString());
		}

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

	private MembershipResponse doCreate(UUID organizationId, UUID userId, MembershipTemplate template) {
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
				.membershipTemplate(template)
				.build();
		m = membershipRepository.save(m);
		return MembershipResponse.from(m);
	}

	/**
	 * Sets (or replaces) the template on an existing membership and creates
	 * {@link com.akandiah.propmanager.features.membership.domain.MemberScope} binding
	 * rows for any resource IDs supplied.  Existing scope rows are kept; new ones are
	 * created only where none already exist for a given (scopeType, scopeId) pair.
	 */
	@Transactional
	public MembershipResponse applyTemplate(UUID membershipId, ApplyTemplateRequest request) {
		Membership membership = membershipRepository.findById(membershipId)
				.orElseThrow(() -> new ResourceNotFoundException("Membership", membershipId));

		MembershipTemplate template = membershipTemplateRepository.findById(request.templateId())
				.orElseThrow(() -> new ResourceNotFoundException("MembershipTemplate", request.templateId()));

		membership.setMembershipTemplate(template);
		membershipRepository.save(membership);

		if (request.resourceIds() != null) {
			request.resourceIds().forEach((scopeType, ids) -> {
				for (UUID resourceId : ids) {
					boolean exists = memberScopeRepository.existsByMembershipIdAndScopeTypeAndScopeId(
							membershipId, scopeType, resourceId);
					if (!exists) {
						memberScopeService.createWithoutEvent(membershipId,
								new CreateMemberScopeRequest(scopeType, resourceId, null));
					}
				}
			});
		}

		if (membership.getUser() != null) {
			eventPublisher.publishEvent(new PermissionsChangedEvent(Set.of(membership.getUser().getId())));
		}

		return MembershipResponse.from(membershipRepository.findById(membershipId).orElseThrow());
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
