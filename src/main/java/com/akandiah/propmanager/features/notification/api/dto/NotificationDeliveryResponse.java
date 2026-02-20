package com.akandiah.propmanager.features.notification.api.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.akandiah.propmanager.common.notification.NotificationChannel;
import com.akandiah.propmanager.common.notification.NotificationReferenceType;
import com.akandiah.propmanager.common.notification.NotificationType;
import com.akandiah.propmanager.features.notification.domain.NotificationDelivery;
import com.akandiah.propmanager.features.notification.domain.NotificationDeliveryStatus;

public record NotificationDeliveryResponse(
		UUID id,
		Integer version,
		UUID userId,
		String recipientAddress,
		NotificationType notificationType,
		NotificationChannel channel,
		UUID referenceId,
		NotificationReferenceType referenceType,
		Map<String, Object> templateContext,
		NotificationDeliveryStatus status,
		int retryCount,
		Instant sentAt,
		Instant viewedAt,
		Instant createdAt,
		Instant updatedAt
) {
	public static NotificationDeliveryResponse from(NotificationDelivery d) {
		return new NotificationDeliveryResponse(
				d.getId(), d.getVersion(), d.getUserId(), d.getRecipientAddress(),
				d.getNotificationType(), d.getChannel(), d.getReferenceId(), d.getReferenceType(),
				d.getTemplateContext(), d.getStatus(), d.getRetryCount(),
				d.getSentAt(), d.getViewedAt(), d.getCreatedAt(), d.getUpdatedAt()
		);
	}
}
