package com.akandiah.propmanager.features.invite.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.akandiah.propmanager.common.notification.NotificationService;
import com.akandiah.propmanager.common.notification.NotificationTemplate;
import com.akandiah.propmanager.config.NotificationProperties;
import com.akandiah.propmanager.features.invite.domain.EmailDeliveryStatus;
import com.akandiah.propmanager.features.invite.domain.Invite;
import com.akandiah.propmanager.features.invite.domain.InviteEmailRequestedEvent;
import com.akandiah.propmanager.features.invite.domain.InviteRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Sends invite emails after the originating transaction commits.
 *
 * <p>Runs on the async task executor so the HTTP thread is never blocked by SMTP.
 * Opens its own REQUIRES_NEW transaction to write SENT/FAILED status back to the invite row.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class InviteEmailListener {

	private final InviteRepository inviteRepository;
	private final NotificationService notificationService;
	private final NotificationProperties notificationProperties;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Async("notificationExecutor")
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void onInviteEmailRequested(InviteEmailRequestedEvent event) {
		Invite invite = inviteRepository.findById(event.inviteId()).orElse(null);
		if (invite == null) {
			log.error("Invite not found for email dispatch: id={}", event.inviteId());
			return;
		}

		try {
			sendEmail(invite, event.metadata());
			if (event.isResend()) {
				invite.setLastResentAt(Instant.now());
			} else {
				invite.setSentAt(Instant.now());
			}
			invite.setEmailStatus(EmailDeliveryStatus.SENT);
			invite.setEmailError(null);
			log.info("Invite email sent: id={}, isResend={}", invite.getId(), event.isResend());
		} catch (Exception e) {
			log.warn("Failed to send invite email: id={}, attempt={}", invite.getId(), invite.getEmailRetryCount() + 1, e);
			invite.setEmailRetryCount(invite.getEmailRetryCount() + 1);
			invite.setEmailStatus(EmailDeliveryStatus.FAILED);
			invite.setEmailError(e.getMessage());
		}

		inviteRepository.save(invite);
	}

	private void sendEmail(Invite invite, Map<String, Object> metadata) {
		String inviteLink = notificationProperties.baseUrl() + "/invite/accept?token=" + invite.getToken();

		Map<String, Object> emailContext = new HashMap<>(metadata != null ? metadata : Map.of());
		emailContext.put("inviteLink", inviteLink);
		emailContext.put("inviterName", invite.getInvitedBy().getName());
		emailContext.put("role", invite.getRole());
		emailContext.put("expiresAt", invite.getExpiresAt());

		NotificationTemplate template = switch (invite.getTargetType()) {
			case LEASE -> NotificationTemplate.INVITE_LEASE;
			case PROPERTY -> NotificationTemplate.INVITE_PROPERTY;
			default -> NotificationTemplate.INVITE_LEASE;
		};

		notificationService.send(invite.getEmail(), template, emailContext);
	}
}
