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
	 * Create a delivery record and attempt to send immediately.
	 * If the user has opted out of this notification type, the record is not created.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void createAndSend(
			UUID userId,
			String recipientAddress,
			NotificationType type,
			NotificationChannel channel,
			UUID referenceId,
			String referenceType,
			Map<String, Object> templateContext) {

		// Check opt-out preference for known users
		if (userId != null && type.isOptOutAllowed()) {
			boolean disabled = preferenceRepository
					.findByUserIdAndNotificationTypeAndChannel(userId, type, channel)
					.map(p -> !p.isEnabled())
					.orElse(false);
			if (disabled) {
				log.info("Skipping notification: user={} has opted out of type={}", userId, type);
				return;
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
		attemptSend(delivery, templateContext);
		deliveryRepository.save(delivery);
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
	 * Retry a previously failed delivery.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void retryDelivery(UUID deliveryId) {
		NotificationDelivery delivery = deliveryRepository.findById(deliveryId)
				.orElseThrow(() -> new ResourceNotFoundException("NotificationDelivery", deliveryId));

		if (delivery.getStatus() != NotificationDeliveryStatus.FAILED) {
			log.warn("Retry skipped: delivery {} is not in FAILED state (status={})", deliveryId, delivery.getStatus());
			return;
		}

		attemptSend(delivery, delivery.getTemplateContext());
		deliveryRepository.save(delivery);
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
