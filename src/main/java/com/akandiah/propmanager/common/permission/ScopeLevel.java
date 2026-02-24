package com.akandiah.propmanager.common.permission;

import java.util.UUID;

/** One level in the resource hierarchy (scopeType + scopeId). */
public record ScopeLevel(ResourceType scopeType, UUID scopeId) {}
