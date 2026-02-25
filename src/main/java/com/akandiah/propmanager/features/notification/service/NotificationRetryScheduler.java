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
 * Polls for notification deliveries that need recovery and re-attempts them.
 *
 * <p>
 * Two cases are handled:
 * <ol>
 * <li>FAILED deliveries that are eligible for retry (under max retries, cooled
 * off).</li>
 * <li>Stuck PENDING deliveries — rows written by the outbox step whose async
 * send was
 * never executed (JVM crash or queue overflow). These are recovered by treating
 * them the same as a retry.</li>
 * </ol>
 *
 * <p>
 * Interval and retry cap are controlled by
 * {@code app.invite.email-retry-interval-minutes}
 * and {@code app.invite.max-email-retries}. Failures in one delivery do not
 * abort the rest.
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
		Duration retryInterval = Duration.ofMinutes(inviteProperties.emailRetryIntervalMinutes());
		Instant retryBefore = Instant.now().minus(retryInterval);

		// Retry FAILED deliveries that have cooled off
		List<NotificationDelivery> retryable = deliveryRepository.findRetryableFailedDeliveries(
				inviteProperties.maxEmailRetries(), retryBefore);

		if (!retryable.isEmpty()) {
			log.info("Retrying {} failed notification delivery(ies)", retryable.size());
			for (NotificationDelivery delivery : retryable) {
				try {
					deliveryService.sendDelivery(delivery.getId());
				} catch (Exception e) {
					log.error("Retry failed for delivery id={}: {}", delivery.getId(), e.getMessage(), e);
				}
			}
		}

		// Recover stuck PENDING deliveries (outbox rows whose async send was lost)
		Instant stuckBefore = Instant.now().minus(retryInterval);
		List<NotificationDelivery> stuck = deliveryRepository.findStuckPendingDeliveries(stuckBefore);

		if (!stuck.isEmpty()) {
			log.info("Recovering {} stuck PENDING notification delivery(ies)", stuck.size());
			for (NotificationDelivery delivery : stuck) {
				try {
					deliveryService.sendDelivery(delivery.getId());
				} catch (Exception e) {
					log.error("Recovery failed for stuck delivery id={}: {}", delivery.getId(), e.getMessage(), e);
				}
			}
		}
	}
}
