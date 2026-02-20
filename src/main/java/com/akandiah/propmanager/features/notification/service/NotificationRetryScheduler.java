package com.akandiah.propmanager.features.notification.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.akandiah.propmanager.config.InviteProperties;
import com.akandiah.propmanager.features.notification.domain.NotificationDelivery;
import com.akandiah.propmanager.features.notification.domain.NotificationDeliveryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Polls for notification deliveries that failed and re-attempts them.
 *
 * <p>Interval and retry cap are controlled by {@code app.invite.email-retry-interval-minutes}
 * and {@code app.invite.max-email-retries}. Failures in one retry do not abort the rest.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationRetryScheduler {

	private final NotificationDeliveryRepository deliveryRepository;
	private final NotificationDeliveryService deliveryService;
	private final InviteProperties inviteProperties;

	@Scheduled(fixedDelayString = "${app.invite.email-retry-interval-minutes:15}m")
	public void retryFailedDeliveries() {
		Instant retryBefore = Instant.now().minus(Duration.ofMinutes(inviteProperties.emailRetryIntervalMinutes()));
		List<NotificationDelivery> retryable = deliveryRepository.findRetryableFailedDeliveries(
				inviteProperties.maxEmailRetries(), retryBefore);

		if (retryable.isEmpty()) {
			return;
		}

		log.info("Retrying {} failed notification delivery(ies)", retryable.size());

		for (NotificationDelivery delivery : retryable) {
			try {
				deliveryService.retryDelivery(delivery.getId());
			} catch (Exception e) {
				log.error("Retry failed for delivery id={}: {}", delivery.getId(), e.getMessage(), e);
			}
		}
	}
}
