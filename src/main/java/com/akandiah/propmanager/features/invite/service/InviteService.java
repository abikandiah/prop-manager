package com.akandiah.propmanager.features.invite.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.common.notification.NotificationService;
import com.akandiah.propmanager.common.notification.NotificationTemplate;
import com.akandiah.propmanager.features.invite.api.dto.InviteResponse;
import com.akandiah.propmanager.features.invite.domain.Invite;
import com.akandiah.propmanager.features.invite.domain.InviteAcceptedEvent;
import com.akandiah.propmanager.features.invite.domain.InviteRepository;
import com.akandiah.propmanager.features.invite.domain.InviteStatus;
import com.akandiah.propmanager.features.invite.domain.TargetType;
import com.akandiah.propmanager.features.user.domain.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing invitations.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InviteService {

	private final InviteRepository inviteRepository;
	private final NotificationService notificationService;
	private final ApplicationEventPublisher eventPublisher;

	private static final SecureRandom RANDOM = new SecureRandom();

	@Value("${app.notification.base-url}")
	private String baseUrl;

	@Value("${app.invite.expiry-hours:72}")
	private int expiryHours;

	@Value("${app.invite.resend-cooldown-minutes:15}")
	private int resendCooldownMinutes;

	/**
	 * Create and send an invitation.
	 *
	 * @param email      Recipient email address
	 * @param targetType Type of resource being invited to
	 * @param targetId   ID of the resource
	 * @param role       Role/permission level
	 * @param invitedBy  User sending the invite
	 * @param metadata   Additional context data for the email template
	 * @return Created invite
	 */
	@Transactional
	public InviteResponse createAndSendInvite(String email, TargetType targetType, UUID targetId, String role, User invitedBy,
			Map<String, Object> metadata) {

		// Check if a pending invite already exists
		if (inviteRepository.existsByEmailAndTargetTypeAndTargetIdAndStatus(email, targetType, targetId,
				InviteStatus.PENDING)) {
			throw new IllegalStateException("An active invitation already exists for this email and resource");
		}

		// Generate secure token
		String token = generateSecureToken();

		// Create invite
		Instant now = Instant.now();
		Instant expiresAt = now.plus(Duration.ofHours(expiryHours));

		Invite invite = Invite.builder()
				.email(email)
				.token(token)
				.targetType(targetType)
				.targetId(targetId)
				.role(role)
				.invitedBy(invitedBy)
				.status(InviteStatus.PENDING)
				.expiresAt(expiresAt)
				.build();

		invite = inviteRepository.save(invite);

		// Send email notification
		sendInviteEmail(invite, metadata);

		// Update sent timestamp
		invite.setSentAt(Instant.now());
		invite = inviteRepository.save(invite);

		log.info("Invite created and sent: id={}, email={}, targetType={}, targetId={}", invite.getId(), email,
				targetType, targetId);

		return InviteResponse.from(invite);
	}

	/**
	 * Resend an existing invitation.
	 *
	 * @param inviteId Invite to resend
	 * @param metadata Additional context data for the email template
	 * @return Updated invite
	 */
	@Transactional
	public InviteResponse resendInvite(UUID inviteId, Map<String, Object> metadata) {
		Invite invite = inviteRepository.findById(inviteId)
				.orElseThrow(() -> new ResourceNotFoundException("Invite", inviteId));

		// Validate invite can be resent
		if (invite.getStatus() != InviteStatus.PENDING) {
			throw new IllegalStateException("Only pending invites can be resent");
		}

		if (invite.isExpired()) {
			invite.setStatus(InviteStatus.EXPIRED);
			inviteRepository.save(invite);
			throw new IllegalStateException("Cannot resend expired invite");
		}

		// Check resend cooldown
		if (invite.getLastResentAt() != null) {
			Instant cooldownExpiry = invite.getLastResentAt().plus(Duration.ofMinutes(resendCooldownMinutes));
			if (Instant.now().isBefore(cooldownExpiry)) {
				throw new IllegalStateException("Please wait before resending this invitation");
			}
		}

		// Send email
		sendInviteEmail(invite, metadata);

		// Update timestamp
		invite.setLastResentAt(Instant.now());
		invite = inviteRepository.save(invite);

		log.info("Invite resent: id={}, email={}", inviteId, invite.getEmail());

		return InviteResponse.from(invite);
	}

	/**
	 * Redeem an invitation by token.
	 *
	 * @param token      Invitation token
	 * @param claimedBy  User who is claiming the invite
	 * @return Accepted invite
	 */
	@Transactional
	public InviteResponse redeemInvite(String token, User claimedBy) {
		Invite invite = inviteRepository.findByToken(token)
				.orElseThrow(() -> new ResourceNotFoundException("Invite not found or invalid token"));

		// Validate invite
		if (invite.getStatus() != InviteStatus.PENDING) {
			throw new IllegalStateException("This invitation has already been " + invite.getStatus().name().toLowerCase());
		}

		if (invite.isExpired()) {
			invite.setStatus(InviteStatus.EXPIRED);
			inviteRepository.save(invite);
			throw new IllegalStateException("This invitation has expired");
		}

		// Accept invite
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
	@Transactional(readOnly = true)
	public List<InviteResponse> findInvitesByTarget(TargetType targetType, UUID targetId) {
		return inviteRepository.findByTargetTypeAndTargetId(targetType, targetId).stream()
				.map(InviteResponse::from)
				.toList();
	}

	/**
	 * Find all invites for a specific email.
	 */
	@Transactional(readOnly = true)
	public List<InviteResponse> findInvitesByEmail(String email) {
		return inviteRepository.findByEmail(email).stream()
				.map(InviteResponse::from)
				.toList();
	}

	/**
	 * Find an invite by ID.
	 */
	@Transactional(readOnly = true)
	public InviteResponse findById(UUID inviteId) {
		Invite invite = inviteRepository.findById(inviteId)
				.orElseThrow(() -> new ResourceNotFoundException("Invite", inviteId));
		return InviteResponse.from(invite);
	}

	/**
	 * Expire all pending invites that have passed their expiration date.
	 * This should be called periodically (e.g., via scheduled job).
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

	/**
	 * Send the invitation email using the notification service.
	 */
	private void sendInviteEmail(Invite invite, Map<String, Object> metadata) {
		String inviteLink = baseUrl + "/invite/accept?token=" + invite.getToken();

		Map<String, Object> emailContext = new HashMap<>(metadata != null ? metadata : Map.of());
		emailContext.put("inviteLink", inviteLink);
		emailContext.put("inviterName", invite.getInvitedBy().getName());
		emailContext.put("role", invite.getRole());
		emailContext.put("expiresAt", invite.getExpiresAt());

		NotificationTemplate template = switch (invite.getTargetType()) {
		case LEASE -> NotificationTemplate.INVITE_LEASE;
		case PROPERTY -> NotificationTemplate.INVITE_PROPERTY;
		default -> NotificationTemplate.INVITE_LEASE; // Default fallback
		};

		notificationService.sendAsync(invite.getEmail(), template, emailContext);
	}

	/**
	 * Generate a secure random token for invites.
	 */
	private String generateSecureToken() {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}
