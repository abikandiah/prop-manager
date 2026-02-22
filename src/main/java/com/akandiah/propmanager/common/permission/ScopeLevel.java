package com.akandiah.propmanager.common.permission;

import java.util.UUID;

/**
 * One level in the resource hierarchy (scopeType + scopeId).
 * Used when resolving org → property → unit for permission checks.
 */
public record ScopeLevel(String scopeType, UUID scopeId) {}
