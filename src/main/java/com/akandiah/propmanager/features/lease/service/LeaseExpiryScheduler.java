package com.akandiah.propmanager.features.lease.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.features.auth.domain.PermissionsChangedEvent;
import com.akandiah.propmanager.features.lease.domain.Lease;
import com.akandiah.propmanager.features.lease.domain.LeaseLifecycleEvent;
import com.akandiah.propmanager.features.lease.domain.LeaseLifecycleEventType;
import com.akandiah.propmanager.features.lease.domain.LeaseRepository;
import com.akandiah.propmanager.features.lease.domain.LeaseStatus;
import com.akandiah.propmanager.features.lease.domain.LeaseTenantRepository;
import com.akandiah.propmanager.features.unit.domain.UnitRepository;
import com.akandiah.propmanager.features.unit.domain.UnitStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduled jobs for lease expiry lifecycle management.
 * Sends advance expiry notifications and auto-transitions overdue leases.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LeaseExpiryScheduler {

	private final LeaseRepository leaseRepository;
	private final LeaseTenantRepository leaseTenantRepository;
	private final UnitRepository unitRepository;
	private final ApplicationEventPublisher eventPublisher;

	/** Sends advance expiry notifications for leases ending in 30 days. Runs daily at 9 AM. */
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

	/** Transitions ACTIVE leases past their endDate to EXPIRED. Runs daily at midnight. */
	@Scheduled(cron = "0 0 0 * * *")
	@Transactional
	public void expireLeases() {
		LocalDate today = LocalDate.now();
		List<Lease> toExpire = leaseRepository.findByStatusAndEndDateBefore(LeaseStatus.ACTIVE, today);

		if (toExpire.isEmpty()) {
			return;
		}

		for (Lease lease : toExpire) {
			lease.setStatus(LeaseStatus.EXPIRED);
			leaseRepository.save(lease);

			unitRepository.findById(lease.getUnit().getId()).ifPresent(unit -> {
				unit.setStatus(UnitStatus.VACANT);
				unitRepository.save(unit);
			});

			Set<UUID> tenantUserIds = leaseTenantRepository.findByLease_IdWithTenantUser(lease.getId()).stream()
					.map(lt -> lt.getTenant().getUser().getId())
					.collect(Collectors.toSet());
			if (!tenantUserIds.isEmpty()) {
				eventPublisher.publishEvent(new PermissionsChangedEvent(tenantUserIds));
			}

			eventPublisher.publishEvent(
					new LeaseLifecycleEvent(lease.getId(), LeaseLifecycleEventType.EXPIRED));
		}

		log.info("Expired {} lease(s) as of {}", toExpire.size(), today);
	}
}
