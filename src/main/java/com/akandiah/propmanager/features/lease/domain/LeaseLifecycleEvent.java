package com.akandiah.propmanager.features.lease.domain;

import java.util.UUID;

public record LeaseLifecycleEvent(UUID leaseId, LeaseLifecycleEventType type) {}
