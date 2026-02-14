package com.akandiah.propmanager.features.invite.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduled task to automatically expire old invitations.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class InviteExpiryScheduledTask {

	private final InviteService inviteService;

	/**
	 * Expire old pending invites every hour.
	 * Runs at the top of every hour (e.g., 01:00, 02:00, 03:00...).
	 */
	@Scheduled(cron = "0 0 * * * *")
	public void expireOldInvites() {
		log.info("Running scheduled task: expiring old invites");

		try {
			int expiredCount = inviteService.expireOldInvites();
			log.info("Scheduled task completed: {} invites expired", expiredCount);
		} catch (Exception e) {
			log.error("Error during scheduled invite expiry task", e);
		}
	}
}
