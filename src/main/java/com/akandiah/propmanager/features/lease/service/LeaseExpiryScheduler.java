package com.akandiah.propmanager.features.lease.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.features.lease.domain.Lease;
import com.akandiah.propmanager.features.lease.domain.LeaseLifecycleEvent;
import com.akandiah.propmanager.features.lease.domain.LeaseLifecycleEventType;
import com.akandiah.propmanager.features.lease.domain.LeaseRepository;
import com.akandiah.propmanager.features.lease.domain.LeaseStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Sends advance expiry notifications for leases ending in 30 days.
 * Runs daily at 9 AM. The {@code @Transactional} annotation ensures
 * the AFTER_COMMIT phase fires for each published event.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LeaseExpiryScheduler {

	private final LeaseRepository leaseRepository;
	private final ApplicationEventPublisher eventPublisher;

	@Scheduled(cron = "0 0 9 * * *")
	@Transactional
	public void notifyExpiringLeases() {
		LocalDate targetDate = LocalDate.now().plusDays(30);
		List<Lease> expiring = leaseRepository.findByStatusAndEndDate(LeaseStatus.ACTIVE, targetDate);

		if (expiring.isEmpty()) {
			return;
		}

		log.info("Publishing EXPIRING_SOON events for {} lease(s) ending on {}", expiring.size(), targetDate);

		for (Lease lease : expiring) {
			eventPublisher.publishEvent(new LeaseLifecycleEvent(lease.getId(), LeaseLifecycleEventType.EXPIRING_SOON));
		}
	}
}
