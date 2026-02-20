package com.akandiah.propmanager.features.notification.service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.akandiah.propmanager.common.notification.NotificationChannel;
import com.akandiah.propmanager.common.notification.NotificationReferenceType;
import com.akandiah.propmanager.common.notification.NotificationType;
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
 * to {@link NotificationDeliveryService} and
 * {@link NotificationDeliverySender}.
 *
 * <p>
 * Outbox pattern: each handler runs synchronously on the committing thread
 * (AFTER_COMMIT, no @Async). It first commits a PENDING row via
 * {@code deliveryService.createPending()} (REQUIRES_NEW), then enqueues the
 * async
 * send via {@code notificationSender.sendAsync()}. A JVM crash between commit
 * and
 * enqueue leaves a recoverable PENDING row that the scheduler will pick up.
 *
 * <p>
 * No {@code @Transactional} here — each call to the delivery service opens its
 * own {@code REQUIRES_NEW} transaction, isolating failures per recipient.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationDispatcher {

	private final NotificationDeliveryService deliveryService;
	private final NotificationDeliverySender notificationSender;
	private final InviteRepository inviteRepository;
	private final LeaseRepository leaseRepository;
	private final LeaseTenantRepository leaseTenantRepository;
	private final UserRepository userRepository;

	private static final DateTimeFormatter INVITE_DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
			.withZone(ZoneId.systemDefault());

	// ─────────────────────────── Invite ───────────────────────────

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onInviteEmailRequested(InviteEmailRequestedEvent event) {
		Invite invite = inviteRepository.findWithInvitedByById(event.inviteId()).orElse(null);
		if (invite == null) {
			log.error("Invite not found for notification dispatch: id={}", event.inviteId());
			return;
		}

		Map<String, Object> context = new HashMap<>(event.metadata() != null ? event.metadata() : Map.of());
		context.put("inviterName", invite.getInvitedBy().getName());
		context.put("role", invite.getRole());
		context.put("expiresAt", INVITE_DATE_FORMAT.format(invite.getExpiresAt()));

		NotificationType type = invite.getTargetType() == TargetType.PROPERTY
				? NotificationType.INVITE_PROPERTY
				: NotificationType.INVITE_LEASE;

		// On resend: cancel any active deliveries to prevent duplicate sends
		if (event.isResend()) {
			deliveryService.cancelActiveDeliveriesForReference(NotificationReferenceType.INVITE, invite.getId());
		}

		UUID deliveryId = deliveryService.createPending(
				null,
				invite.getEmail(),
				type,
				NotificationChannel.EMAIL,
				invite.getId(),
				NotificationReferenceType.INVITE,
				context);

		if (deliveryId != null) {
			notificationSender.sendAsync(deliveryId);
		}
	}

	// ─────────────────────────── Lease lifecycle ───────────────────────────

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
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
				"eventType", event.type().name());

		List<LeaseTenant> tenants = leaseTenantRepository.findByLease_IdWithTenantUser(event.leaseId());
		if (tenants.isEmpty()) {
			log.info("No accepted tenants for lease={}, skipping lifecycle notification type={}", event.leaseId(),
					type);
			return;
		}

		for (LeaseTenant lt : tenants) {
			User user = lt.getTenant().getUser();
			try {
				UUID deliveryId = deliveryService.createPending(
						user.getId(),
						user.getEmail(),
						type,
						NotificationChannel.EMAIL,
						lease.getId(),
						NotificationReferenceType.LEASE,
						context);
				if (deliveryId != null) {
					notificationSender.sendAsync(deliveryId);
				}
			} catch (Exception e) {
				log.error("Failed to dispatch lease lifecycle notification: leaseId={}, userId={}", lease.getId(),
						user.getId(), e);
			}
		}
	}

	// ─────────────────────────── User registered ───────────────────────────

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onUserRegistered(UserRegisteredEvent event) {
		User user = userRepository.findById(event.userId()).orElse(null);
		if (user == null) {
			log.error("User not found for notification dispatch: id={}", event.userId());
			return;
		}

		Map<String, Object> context = Map.of(
				"name", user.getName(),
				"email", user.getEmail());

		UUID deliveryId = deliveryService.createPending(
				user.getId(),
				user.getEmail(),
				NotificationType.ACCOUNT_CREATED,
				NotificationChannel.EMAIL,
				user.getId(),
				NotificationReferenceType.USER,
				context);

		if (deliveryId != null) {
			notificationSender.sendAsync(deliveryId);
		}
	}
}
