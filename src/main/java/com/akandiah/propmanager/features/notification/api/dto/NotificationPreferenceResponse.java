package com.akandiah.propmanager.features.notification.api.dto;

import com.akandiah.propmanager.common.notification.NotificationChannel;
import com.akandiah.propmanager.common.notification.NotificationType;
import com.akandiah.propmanager.features.notification.domain.UserNotificationPreference;

import java.util.UUID;

public record NotificationPreferenceResponse(
		UUID id,
		Integer version,
		NotificationType notificationType,
		NotificationChannel channel,
		boolean enabled,
		boolean optOutAllowed
) {

	public static NotificationPreferenceResponse from(UserNotificationPreference pref) {
		return new NotificationPreferenceResponse(
				pref.getId(),
				pref.getVersion(),
				pref.getNotificationType(),
				pref.getChannel(),
				pref.isEnabled(),
				pref.getNotificationType().isOptOutAllowed()
		);
	}

	public static NotificationPreferenceResponse defaultFor(NotificationType type, NotificationChannel channel) {
		return new NotificationPreferenceResponse(
				null,
				null,
				type,
				channel,
				true,
				type.isOptOutAllowed()
		);
	}
}
