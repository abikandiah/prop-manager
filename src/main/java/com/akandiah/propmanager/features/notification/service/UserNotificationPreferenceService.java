package com.akandiah.propmanager.features.notification.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.notification.NotificationChannel;
import com.akandiah.propmanager.common.notification.NotificationType;
import com.akandiah.propmanager.features.notification.api.dto.NotificationPreferenceResponse;
import com.akandiah.propmanager.features.notification.domain.UserNotificationPreference;
import com.akandiah.propmanager.features.notification.domain.UserNotificationPreferenceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserNotificationPreferenceService {

	private final UserNotificationPreferenceRepository preferenceRepository;

	/**
	 * Returns preferences for all notification types, synthesising defaults for any
	 * type
	 * that has no persisted row yet.
	 */
	public List<NotificationPreferenceResponse> findByUserId(UUID userId) {
		Map<NotificationType, UserNotificationPreference> persisted = preferenceRepository.findByUserId(userId)
				.stream()
				.collect(Collectors.toMap(UserNotificationPreference::getNotificationType, p -> p));

		return Arrays.stream(NotificationType.values())
				.map(type -> persisted.containsKey(type)
						? NotificationPreferenceResponse.from(persisted.get(type))
						: NotificationPreferenceResponse.defaultFor(type, NotificationChannel.EMAIL))
				.toList();
	}

	/**
	 * Upsert a preference row.
	 * Throws {@link IllegalArgumentException} if the caller tries to disable a
	 * non-opt-out type.
	 */
	@Transactional
	public NotificationPreferenceResponse updatePreference(
			UUID userId, NotificationType type, NotificationChannel channel, boolean enabled) {

		if (!type.getOptOutAllowed() && !enabled) {
			throw new IllegalArgumentException(
					"Notifications of type " + type + " cannot be disabled.");
		}

		UserNotificationPreference pref = preferenceRepository
				.findByUserIdAndNotificationTypeAndChannel(userId, type, channel)
				.orElseGet(() -> UserNotificationPreference.builder()
						.userId(userId)
						.notificationType(type)
						.channel(channel)
						.build());

		pref.setEnabled(enabled);
		return NotificationPreferenceResponse.from(preferenceRepository.save(pref));
	}
}
