package com.akandiah.propmanager.features.notification.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.akandiah.propmanager.common.notification.NotificationChannel;
import com.akandiah.propmanager.common.notification.NotificationType;
import com.akandiah.propmanager.config.NotificationProperties;
import com.akandiah.propmanager.features.invite.domain.Invite;
import com.akandiah.propmanager.features.invite.domain.InviteEmailRequestedEvent;
import com.akandiah.propmanager.features.invite.domain.InviteRepository;
import com.akandiah.propmanager.features.invite.domain.TargetType;
import com.akandiah.propmanager.features.lease.domain.Lease;
import com.akandiah.propmanager.features.lease.domain.LeaseLifecycleEvent;
import com.akandiah.propmanager.features.lease.domain.LeaseTenant;
import com.akandiah.propmanager.features.lease.domain.LeaseTenantRepository;
import com.akandiah.propmanager.features.lease.domain.LeaseRepository;
import com.akandiah.propmanager.features.user.domain.User;
import com.akandiah.propmanager.features.user.domain.UserRegisteredEvent;
import com.akandiah.propmanager.features.user.domain.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Central dispatcher that listens to domain events and delegates delivery
 * to {@link NotificationDeliveryService}. Runs asynchronously after commit so
 * the originating HTTP thread is never blocked by email I/O.
 *
 * <p>No {@code @Transactional} here — each call to
 * {@code deliveryService.createAndSend()} opens its own {@code REQUIRES_NEW}
 * transaction, so failures are isolated per recipient.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationDispatcher {

	private final NotificationDeliveryService deliveryService;
	private final InviteRepository inviteRepository;
	private final LeaseRepository leaseRepository;
	private final LeaseTenantRepository leaseTenantRepository;
	private final UserRepository userRepository;
	private final NotificationProperties notificationProperties;

	// ─────────────────────────── Invite ───────────────────────────

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Async("notificationExecutor")
	public void onInviteEmailRequested(InviteEmailRequestedEvent event) {
		Invite invite = inviteRepository.findWithInvitedByById(event.inviteId()).orElse(null);
		if (invite == null) {
			log.error("Invite not found for notification dispatch: id={}", event.inviteId());
			return;
		}

		String inviteLink = notificationProperties.baseUrl() + "/invite/accept?token=" + invite.getToken();

		Map<String, Object> context = new HashMap<>(event.metadata() != null ? event.metadata() : Map.of());
		context.put("inviteLink", inviteLink);
		context.put("inviterName", invite.getInvitedBy().getName());
		context.put("role", invite.getRole());
		context.put("expiresAt", invite.getExpiresAt());

		NotificationType type = invite.getTargetType() == TargetType.PROPERTY
				? NotificationType.INVITE_PROPERTY
				: NotificationType.INVITE_LEASE;

		deliveryService.createAndSend(
				null,
				invite.getEmail(),
				type,
				NotificationChannel.EMAIL,
				invite.getId(),
				"Invite",
				context);
	}

	// ─────────────────────────── Lease lifecycle ───────────────────────────

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Async("notificationExecutor")
	public void onLeaseLifecycle(LeaseLifecycleEvent event) {
		Lease lease = leaseRepository.findByIdWithUnitAndProperty(event.leaseId()).orElse(null);
		if (lease == null) {
			log.error("Lease not found for notification dispatch: id={}", event.leaseId());
			return;
		}

		NotificationType type = switch (event.type()) {
			case SUBMITTED_FOR_REVIEW -> NotificationType.LEASE_SUBMITTED_FOR_REVIEW;
			case ACTIVATED -> NotificationType.LEASE_ACTIVATED;
			case EXPIRING_SOON -> NotificationType.LEASE_EXPIRING_SOON;
		};

		Map<String, Object> context = Map.of(
				"propertyName", lease.getProperty().getLegalName(),
				"unitNumber", lease.getUnit().getUnitNumber(),
				"startDate", lease.getStartDate(),
				"endDate", lease.getEndDate(),
				"eventType", event.type().name()
		);

		List<LeaseTenant> tenants = leaseTenantRepository.findByLease_IdWithTenantUser(event.leaseId());
		if (tenants.isEmpty()) {
			log.info("No accepted tenants for lease={}, skipping lifecycle notification type={}", event.leaseId(), type);
			return;
		}

		for (LeaseTenant lt : tenants) {
			User user = lt.getTenant().getUser();
			try {
				deliveryService.createAndSend(
						user.getId(),
						user.getEmail(),
						type,
						NotificationChannel.EMAIL,
						lease.getId(),
						"Lease",
						context);
			} catch (Exception e) {
				log.error("Failed to dispatch lease lifecycle notification: leaseId={}, userId={}", lease.getId(), user.getId(), e);
			}
		}
	}

	// ─────────────────────────── User registered ───────────────────────────

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Async("notificationExecutor")
	public void onUserRegistered(UserRegisteredEvent event) {
		User user = userRepository.findById(event.userId()).orElse(null);
		if (user == null) {
			log.error("User not found for notification dispatch: id={}", event.userId());
			return;
		}

		Map<String, Object> context = Map.of(
				"name", user.getName(),
				"email", user.getEmail()
		);

		deliveryService.createAndSend(
				user.getId(),
				user.getEmail(),
				NotificationType.ACCOUNT_CREATED,
				NotificationChannel.EMAIL,
				user.getId(),
				"User",
				context);
	}
}
