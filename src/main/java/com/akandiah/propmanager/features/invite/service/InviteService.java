package com.akandiah.propmanager.features.invite.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.config.InviteProperties;
import com.akandiah.propmanager.features.invite.api.dto.InvitePreviewResponse;
import com.akandiah.propmanager.features.invite.api.dto.InviteResponse;
import com.akandiah.propmanager.features.invite.domain.Invite;
import com.akandiah.propmanager.features.invite.domain.InviteAcceptedEvent;
import com.akandiah.propmanager.features.invite.domain.InviteEmailRequestedEvent;
import com.akandiah.propmanager.features.invite.domain.InviteRepository;
import com.akandiah.propmanager.features.invite.domain.InviteStatus;
import com.akandiah.propmanager.features.invite.domain.TargetType;
import com.akandiah.propmanager.features.user.domain.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing invitations.
 *
 * <p>
 * Email sending is deliberately outside this service's scope.
 * After each DB write commits, an {@link InviteEmailRequestedEvent} is
 * published
 * and handled asynchronously by {@link NotificationDispatcher}, which writes
 * the SENT/FAILED status to notification_deliveries in its own transaction.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InviteService {

	private final InviteRepository inviteRepository;
	private final ApplicationEventPublisher eventPublisher;
	private final InviteProperties inviteProperties;

	private static final SecureRandom RANDOM = new SecureRandom();

	/**
	 * Create and send an invitation.
	 *
	 * @param email      Recipient email address
	 * @param targetType Type of resource being invited to
	 * @param targetId   ID of the resource
	 * @param attributes Domain-specific context persisted on the invite and used
	 *                   for email templates
	 * @param invitedBy  User sending the invite
	 * @return Created invite
	 */
	@Transactional
	public InviteResponse createAndSendInvite(String email, TargetType targetType, UUID targetId,
			Map<String, Object> attributes, User invitedBy) {

		if (inviteRepository.existsByEmailAndTargetTypeAndTargetIdAndStatus(email, targetType, targetId,
				InviteStatus.PENDING)) {
			throw new IllegalStateException("An active invitation already exists for this email and resource");
		}

		Instant now = Instant.now();
		Invite invite = Invite.builder()
				.email(email)
				.token(generateSecureToken())
				.targetType(targetType)
				.targetId(targetId)
				.attributes(attributes != null ? attributes : new HashMap<>())
				.invitedBy(invitedBy)
				.status(InviteStatus.PENDING)
				.expiresAt(now.plus(Duration.ofHours(inviteProperties.expiryHours())))
				.build();

		invite.setSentAt(Instant.now());
		invite = inviteRepository.save(invite);

		// Email is sent after this transaction commits — see NotificationDispatcher
		eventPublisher.publishEvent(new InviteEmailRequestedEvent(invite.getId(), false));

		log.info("Invite created: id={}, email={}, targetType={}, targetId={}", invite.getId(), email, targetType,
				targetId);

		return InviteResponse.from(invite);
	}

	/**
	 * Resend an existing invitation.
	 * Template context is loaded from the persisted invite attributes by the
	 * dispatcher.
	 *
	 * @param inviteId Invite to resend
	 * @return Updated invite
	 */
	@Transactional
	public InviteResponse resendInvite(UUID inviteId) {
		Invite invite = inviteRepository.findById(inviteId)
				.orElseThrow(() -> new ResourceNotFoundException("Invite", inviteId));

		if (invite.getStatus() == InviteStatus.ACCEPTED || invite.getStatus() == InviteStatus.REVOKED) {
			throw new IllegalStateException("Only pending or expired invites can be resent");
		}

		if (invite.getLastResentAt() != null) {
			Instant cooldownExpiry = invite.getLastResentAt()
					.plus(Duration.ofMinutes(inviteProperties.resendCooldownMinutes()));
			if (Instant.now().isBefore(cooldownExpiry)) {
				throw new IllegalStateException("Please wait before resending this invitation");
			}
		}

		// Renew window for expired invites
		if (invite.isExpired() || invite.getStatus() == InviteStatus.EXPIRED) {
			invite.setExpiresAt(Instant.now().plus(Duration.ofHours(inviteProperties.expiryHours())));
			invite.setStatus(InviteStatus.PENDING);
		}

		invite.setLastResentAt(Instant.now());
		invite = inviteRepository.save(invite);

		// Email is sent after this transaction commits — see NotificationDispatcher
		eventPublisher.publishEvent(new InviteEmailRequestedEvent(invite.getId(), true));

		log.info("Invite resend requested: id={}, email={}", inviteId, invite.getEmail());

		return InviteResponse.from(invite);
	}

	/**
	 * Resolve public preview data for an invite token without requiring
	 * authentication.
	 * Email is masked. Preview context is loaded from the snapshot stored in
	 * {@code invite.attributes["preview"]} at invite-creation time — no domain
	 * repository joins are performed here.
	 *
	 * @param token Invitation token
	 * @return Public preview of the invite context
	 */
	public InvitePreviewResponse getPreview(String token) {
		Invite invite = inviteRepository.findByToken(token)
				.orElseThrow(() -> new ResourceNotFoundException("Invite not found or invalid token"));

		@SuppressWarnings("unchecked")
		Map<String, Object> preview = (Map<String, Object>) invite.getAttributes().get("preview");

		return new InvitePreviewResponse(
				maskEmail(invite.getEmail()),
				invite.getStatus(),
				invite.isValid(),
				invite.isExpired(),
				invite.getExpiresAt(),
				invite.getInvitedBy().getName(),
				invite.getTargetType(),
				preview != null ? preview : Map.of());
	}

	/**
	 * Accept an invitation. Requires the authenticated user's email to match the
	 * invited email.
	 *
	 * @param token     Invitation token
	 * @param claimedBy Authenticated user accepting the invite
	 * @return Accepted invite
	 */
	@Transactional
	public InviteResponse acceptInvite(String token, User claimedBy) {
		Invite invite = inviteRepository.findByToken(token)
				.orElseThrow(() -> new ResourceNotFoundException("Invite not found or invalid token"));

		if (invite.getStatus() != InviteStatus.PENDING) {
			throw new IllegalStateException(
					"This invitation has already been " + invite.getStatus().name().toLowerCase());
		}

		if (invite.isExpired()) {
			invite.setStatus(InviteStatus.EXPIRED);
			inviteRepository.save(invite);
			throw new IllegalStateException("This invitation has expired");
		}

		if (!invite.getEmail().equalsIgnoreCase(claimedBy.getEmail())) {
			throw new IllegalStateException("This invitation was sent to a different email address");
		}

		invite.setStatus(InviteStatus.ACCEPTED);
		invite.setAcceptedAt(Instant.now());
		invite.setClaimedUser(claimedBy);
		invite = inviteRepository.save(invite);

		eventPublisher.publishEvent(new InviteAcceptedEvent(invite, claimedBy));

		log.info("Invite accepted: id={}, email={}, claimedBy={}", invite.getId(), invite.getEmail(),
				claimedBy.getId());

		return InviteResponse.from(invite);
	}

	/**
	 * Revoke an invitation (prevent acceptance).
	 *
	 * @param inviteId Invite to revoke
	 */
	@Transactional
	public void revokeInvite(UUID inviteId) {
		Invite invite = inviteRepository.findById(inviteId)
				.orElseThrow(() -> new ResourceNotFoundException("Invite", inviteId));

		if (invite.getStatus() != InviteStatus.PENDING) {
			throw new IllegalStateException("Only pending invites can be revoked");
		}

		invite.setStatus(InviteStatus.REVOKED);
		inviteRepository.save(invite);

		log.info("Invite revoked: id={}, email={}", inviteId, invite.getEmail());
	}

	/**
	 * Find all invites for a specific resource.
	 */
	public List<InviteResponse> findInvitesByTarget(TargetType targetType, UUID targetId) {
		return inviteRepository.findByTargetTypeAndTargetId(targetType, targetId).stream()
				.map(InviteResponse::from)
				.toList();
	}

	/**
	 * Find all invites for a specific email.
	 */
	public List<InviteResponse> findInvitesByEmail(String email) {
		return inviteRepository.findByEmail(email).stream()
				.map(InviteResponse::from)
				.toList();
	}

	/**
	 * Find an invite by ID.
	 */
	public InviteResponse findById(UUID inviteId) {
		Invite invite = inviteRepository.findById(inviteId)
				.orElseThrow(() -> new ResourceNotFoundException("Invite", inviteId));
		return InviteResponse.from(invite);
	}

	/**
	 * Expire all pending invites that have passed their expiration date.
	 * Called periodically via scheduled job.
	 *
	 * @return Number of invites expired
	 */
	@Transactional
	public int expireOldInvites() {
		List<Invite> expiredInvites = inviteRepository.findExpiredPendingInvites(Instant.now());

		for (Invite invite : expiredInvites) {
			invite.setStatus(InviteStatus.EXPIRED);
		}

		inviteRepository.saveAll(expiredInvites);

		log.info("Expired {} old invites", expiredInvites.size());
		return expiredInvites.size();
	}

	private String generateSecureToken() {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String maskEmail(String email) {
		int atIndex = email.indexOf('@');
		if (atIndex < 1) {
			return email;
		}
		return email.charAt(0) + "***" + email.substring(atIndex);
	}
}
