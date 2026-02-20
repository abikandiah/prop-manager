package com.akandiah.propmanager.features.notification.api.dto;

import com.akandiah.propmanager.common.notification.NotificationChannel;
import com.akandiah.propmanager.common.notification.NotificationType;
import jakarta.validation.constraints.NotNull;

public record UpdateNotificationPreferenceRequest(
		@NotNull NotificationType notificationType,
		@NotNull NotificationChannel channel,
		@NotNull Boolean enabled
) {}
