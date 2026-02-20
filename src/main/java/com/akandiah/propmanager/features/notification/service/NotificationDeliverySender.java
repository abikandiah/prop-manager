package com.akandiah.propmanager.features.notification.service;

import java.util.UUID;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Thin async delegate for sending a notification delivery.
 *
 * <p>Spring's proxy-based {@code @Async} requires the call to cross a bean boundary,
 * so this cannot live on {@link NotificationDeliveryService} itself.
 * The outbox pattern guarantees a PENDING row exists before this is called,
 * so a dropped task (queue overflow, JVM crash) is safely recovered by the scheduler.
 */
@Component
@RequiredArgsConstructor
public class NotificationDeliverySender {

	private final NotificationDeliveryService deliveryService;

	@Async("notificationExecutor")
	public void sendAsync(UUID deliveryId) {
		deliveryService.sendDelivery(deliveryId);
	}
}
