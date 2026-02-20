package com.akandiah.propmanager.features.notification.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.common.notification.NotificationChannel;
import com.akandiah.propmanager.common.notification.NotificationReferenceType;
import com.akandiah.propmanager.common.notification.NotificationService;
import com.akandiah.propmanager.common.notification.NotificationType;
import com.akandiah.propmanager.features.notification.api.dto.NotificationDeliveryResponse;
import com.akandiah.propmanager.features.notification.domain.NotificationDelivery;
import com.akandiah.propmanager.features.notification.domain.NotificationDeliveryRepository;
import com.akandiah.propmanager.features.notification.domain.NotificationDeliveryStatus;
import com.akandiah.propmanager.features.notification.domain.UserNotificationPreferenceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles the persistence and delivery of individual notification records.
 * Each method runs in its own REQUIRES_NEW transaction so failures are
 * isolated per recipient and do not roll back the calling context.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationDeliveryService {

	private final NotificationDeliveryRepository deliveryRepository;
	private final UserNotificationPreferenceRepository preferenceRepository;
	private final NotificationService notificationService;

	/**
	 * Returns all deliveries for a user, newest first.
	 */
	@org.springframework.transaction.annotation.Transactional(readOnly = true)
	public List<NotificationDeliveryResponse> findByUserId(UUID userId) {
		return deliveryRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
				.map(NotificationDeliveryResponse::from)
				.toList();
	}

	/**
	 * Writes a PENDING delivery row and returns its ID.
	 * If the user has opted out of this notification type, no record is created and null is returned.
	 * Runs in REQUIRES_NEW so the row is committed before the caller returns.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public UUID createPending(
			UUID userId,
			String recipientAddress,
			NotificationType type,
			NotificationChannel channel,
			UUID referenceId,
			NotificationReferenceType referenceType,
			Map<String, Object> templateContext) {

		// Check opt-out preference for known users
		if (userId != null && type.isOptOutAllowed()) {
			boolean disabled = preferenceRepository
					.findByUserIdAndNotificationTypeAndChannel(userId, type, channel)
					.map(p -> !p.isEnabled())
					.orElse(false);
			if (disabled) {
				log.info("Skipping notification: user={} has opted out of type={}", userId, type);
				return null;
			}
		}

		NotificationDelivery delivery = NotificationDelivery.builder()
				.userId(userId)
				.recipientAddress(recipientAddress)
				.notificationType(type)
				.channel(channel)
				.referenceId(referenceId)
				.referenceType(referenceType)
				.templateContext(templateContext)
				.status(NotificationDeliveryStatus.PENDING)
				.build();

		delivery = deliveryRepository.save(delivery);
		return delivery.getId();
	}

	/**
	 * Sends a PENDING or FAILED delivery by ID.
	 * Loads the delivery, attempts to send, and persists the resulting status.
	 * Runs in REQUIRES_NEW so send failures are isolated per delivery.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void sendDelivery(UUID deliveryId) {
		NotificationDelivery delivery = deliveryRepository.findById(deliveryId)
				.orElseThrow(() -> new ResourceNotFoundException("NotificationDelivery", deliveryId));

		if (delivery.getStatus() != NotificationDeliveryStatus.PENDING
				&& delivery.getStatus() != NotificationDeliveryStatus.FAILED) {
			log.warn("Send skipped: delivery {} is not in PENDING or FAILED state (status={})",
					deliveryId, delivery.getStatus());
			return;
		}

		attemptSend(delivery, delivery.getTemplateContext());
		deliveryRepository.save(delivery);
	}

	/**
	 * Bulk-cancels PENDING and FAILED deliveries for a reference.
	 * Used before a resend to prevent duplicate delivery.
	 * Runs in REQUIRES_NEW so the cancellation is committed atomically.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void cancelActiveDeliveriesForReference(NotificationReferenceType referenceType, UUID referenceId) {
		int cancelled = deliveryRepository.cancelActiveDeliveriesForReference(referenceType, referenceId);
		if (cancelled > 0) {
			log.info("Cancelled {} active delivery(ies) for referenceType={} referenceId={}",
					cancelled, referenceType, referenceId);
		}
	}

	/**
	 * Mark a delivery as viewed by the recipient.
	 * No-op if already marked viewed.
	 */
	@Transactional
	public void markViewed(UUID deliveryId) {
		NotificationDelivery delivery = deliveryRepository.findById(deliveryId)
				.orElseThrow(() -> new ResourceNotFoundException("NotificationDelivery", deliveryId));
		if (delivery.getViewedAt() == null) {
			delivery.setViewedAt(Instant.now());
			deliveryRepository.save(delivery);
		}
	}

	/**
	 * Retry a previously failed delivery. Delegates to sendDelivery.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void retryDelivery(UUID deliveryId) {
		sendDelivery(deliveryId);
	}

	private void attemptSend(NotificationDelivery delivery, Map<String, Object> context) {
		try {
			notificationService.send(delivery.getRecipientAddress(), delivery.getNotificationType().getTemplate(),
					context);
			delivery.setStatus(NotificationDeliveryStatus.SENT);
			delivery.setSentAt(Instant.now());
			delivery.setErrorMessage(null);
			log.info("Notification sent: type={}, to={}", delivery.getNotificationType(), delivery.getRecipientAddress());
		} catch (Exception e) {
			log.warn("Notification failed: type={}, to={}, attempt={}",
					delivery.getNotificationType(), delivery.getRecipientAddress(), delivery.getRetryCount() + 1, e);
			delivery.setRetryCount(delivery.getRetryCount() + 1);
			delivery.setStatus(NotificationDeliveryStatus.FAILED);
			delivery.setErrorMessage(e.getMessage() != null ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 500)) : "Unknown error");
		}
	}
}
