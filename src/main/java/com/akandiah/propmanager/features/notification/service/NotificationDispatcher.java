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
import com.akandiah.propmanager.config.AppProperties;
import com.akandiah.propmanager.features.invite.domain.Invite;
import com.akandiah.propmanager.features.invite.domain.InviteEmailRequestedEvent;
import com.akandiah.propmanager.features.invite.domain.InviteRepository;
import com.akandiah.propmanager.features.lease.domain.Lease;
import com.akandiah.propmanager.features.lease.domain.LeaseLifecycleEvent;
import com.akandiah.propmanager.features.lease.domain.LeaseRepository;
import com.akandiah.propmanager.features.lease.domain.LeaseTenant;
import com.akandiah.propmanager.features.lease.domain.LeaseTenantRepository;
import com.akandiah.propmanager.features.membership.domain.MembershipRepository;
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
	private final MembershipRepository membershipRepository;
	private final UserRepository userRepository;
	private final AppProperties appProperties;

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

		Map<String, Object> context = new HashMap<>(invite.getAttributes() != null ? invite.getAttributes() : Map.of());
		context.put("inviterName", invite.getInvitedBy().getName());
		context.put("inviteLink", appProperties.baseUrl() + "/invite/" + invite.getToken());
		context.put("expiresAt", INVITE_DATE_FORMAT.format(invite.getExpiresAt()));

		NotificationType type = NotificationType.INVITE_LEASE;

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
		Lease lease = leaseRepository.findByIdWithUnitPropertyAndOrg(event.leaseId()).orElse(null);
		if (lease == null) {
			log.error("Lease not found for notification dispatch: id={}", event.leaseId());
			return;
		}

		Map<String, Object> ctx = baseContext(lease);

		switch (event.type()) {
			case SUBMITTED_FOR_REVIEW ->
				notifyTenants(lease, NotificationType.LEASE_SUBMITTED_FOR_REVIEW, ctx);

			case TENANT_SIGNED -> {
				Map<String, Object> signCtx = new HashMap<>(ctx);
				if (event.subjectId() != null) {
					userRepository.findById(event.subjectId())
							.ifPresent(u -> signCtx.put("signingTenantName", u.getName()));
				}
				// Use version-aware count so stale-SIGNED slots after a revert are counted as
				// pending
				long remaining = leaseTenantRepository.countNotSignedForVersion(
						event.leaseId(), lease.getVersion());
				signCtx.put("remainingSignatures", remaining);
				notifyManager(lease, NotificationType.LEASE_TENANT_SIGNED, signCtx);
			}

			case TENANT_ALL_SIGNED ->
				notifyManager(lease, NotificationType.LEASE_ALL_SIGNED, ctx);

			case ACTIVATED ->
				notifyTenants(lease, NotificationType.LEASE_ACTIVATED, ctx);

			case EXPIRING_SOON -> {
				notifyTenants(lease, NotificationType.LEASE_EXPIRING_SOON, ctx);
				notifyManager(lease, NotificationType.LEASE_EXPIRING_SOON, ctx);
			}

			case EXPIRED -> {
				notifyTenants(lease, NotificationType.LEASE_EXPIRED, ctx);
				notifyManager(lease, NotificationType.LEASE_EXPIRED, ctx);
			}

			case TERMINATED ->
				notifyTenants(lease, NotificationType.LEASE_TERMINATED, ctx);

			case EVICTED ->
				notifyTenants(lease, NotificationType.LEASE_EVICTED, ctx);
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

	// ─────────────────────────── Helpers ───────────────────────────

	/**
	 * Builds the shared context map included in all lease lifecycle notification
	 * templates.
	 */
	private Map<String, Object> baseContext(Lease lease) {
		Map<String, Object> ctx = new HashMap<>();
		ctx.put("propertyName", lease.getProperty().getLegalName());
		ctx.put("unitNumber", lease.getUnit().getUnitNumber());
		ctx.put("startDate", lease.getStartDate());
		ctx.put("endDate", lease.getEndDate());
		ctx.put("eventType", "lease");
		return ctx;
	}

	/**
	 * Sends a notification to all accepted (REGISTERED or SIGNED) tenants on this
	 * lease.
	 */
	private void notifyTenants(Lease lease, NotificationType type, Map<String, Object> ctx) {
		List<LeaseTenant> tenants = leaseTenantRepository.findByLease_IdWithTenantUser(lease.getId());
		if (tenants.isEmpty()) {
			log.info("No accepted tenants for lease={}, skipping notification type={}", lease.getId(), type);
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
						ctx);
				if (deliveryId != null) {
					notificationSender.sendAsync(deliveryId);
				}
			} catch (Exception e) {
				log.error("Failed to dispatch tenant notification: leaseId={}, userId={}", lease.getId(), user.getId(),
						e);
			}
		}
	}

	/**
	 * Sends a notification to all org members (managers/admins) of the lease's
	 * property.
	 */
	private void notifyManager(Lease lease, NotificationType type, Map<String, Object> ctx) {
		UUID orgId = lease.getProperty().getOrganization().getId();
		List<User> managers = membershipRepository.findUsersByOrganizationId(orgId);

		if (managers.isEmpty()) {
			log.info("No managers found for org={}, skipping manager notification type={}", orgId, type);
			return;
		}

		for (User manager : managers) {
			try {
				UUID deliveryId = deliveryService.createPending(
						manager.getId(),
						manager.getEmail(),
						type,
						NotificationChannel.EMAIL,
						lease.getId(),
						NotificationReferenceType.LEASE,
						ctx);
				if (deliveryId != null) {
					notificationSender.sendAsync(deliveryId);
				}
			} catch (Exception e) {
				log.error("Failed to dispatch manager notification: leaseId={}, userId={}", lease.getId(),
						manager.getId(), e);
			}
		}
	}
}
