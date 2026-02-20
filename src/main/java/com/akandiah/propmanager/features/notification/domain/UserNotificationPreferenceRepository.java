package com.akandiah.propmanager.features.notification.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.akandiah.propmanager.common.notification.NotificationChannel;
import com.akandiah.propmanager.common.notification.NotificationType;

public interface UserNotificationPreferenceRepository extends JpaRepository<UserNotificationPreference, UUID> {

	List<UserNotificationPreference> findByUserId(UUID userId);

	Optional<UserNotificationPreference> findByUserIdAndNotificationTypeAndChannel(
			UUID userId, NotificationType notificationType, NotificationChannel channel);
}
