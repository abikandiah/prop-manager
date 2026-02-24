package com.akandiah.propmanager.features.auth.domain;

import java.util.Set;
import java.util.UUID;

/**
 * Published after a mutation that may change a user's access list.
 * Listeners evict the affected users from the permissions cache.
 */
public record PermissionsChangedEvent(Set<UUID> affectedUserIds) {
}
