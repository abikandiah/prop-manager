package com.akandiah.propmanager.features.auth.service;

import java.util.Set;
import java.util.UUID;

import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.akandiah.propmanager.features.auth.domain.PermissionsChangedEvent;
import com.akandiah.propmanager.features.invite.domain.InviteAcceptedEvent;
import com.akandiah.propmanager.features.invite.domain.TargetType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Evicts entries from the "permissions" cache when user access may have changed.
 * Listens for {@link PermissionsChangedEvent} (explicit permission mutations)
 * and {@link InviteAcceptedEvent} (tenant gains unit-level READ on acceptance).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PermissionsCacheEvictionListener {

	private final CacheManager cacheManager;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onPermissionsChanged(PermissionsChangedEvent event) {
		evict(event.affectedUserIds());
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onInviteAccepted(InviteAcceptedEvent event) {
		if (event.invite().getTargetType() != TargetType.LEASE) {
			return;
		}
		evict(Set.of(event.claimedUser().getId()));
	}

	private void evict(Set<UUID> userIds) {
		if (userIds.isEmpty()) {
			return;
		}
		var cache = cacheManager.getCache("permissions");
		if (cache == null) {
			return;
		}
		for (UUID userId : userIds) {
			cache.evict(userId);
		}
		log.debug("Evicted permissions cache for {} user(s)", userIds.size());
	}
}
