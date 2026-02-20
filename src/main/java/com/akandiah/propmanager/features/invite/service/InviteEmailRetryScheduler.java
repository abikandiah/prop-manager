package com.akandiah.propmanager.features.invite.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Polls for invite emails that failed delivery and re-dispatches them.
 *
 * <p>Interval and retry cap are controlled by {@code app.invite.email-retry-interval-minutes}
 * and {@code app.invite.max-email-retries}. Once an invite reaches the cap it stays FAILED
 * and is no longer picked up here — manual intervention (or a user-initiated resend) is required.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class InviteEmailRetryScheduler {

	private final InviteService inviteService;

	@Scheduled(fixedDelayString = "${app.invite.email-retry-interval-minutes:15}m")
	public void retryFailedEmails() {
		int count = inviteService.retryFailedEmails();
		if (count > 0) {
			log.info("Retry dispatched for {} failed invite email(s)", count);
		}
	}
}
