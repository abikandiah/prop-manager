package com.akandiah.propmanager.features.organization.domain;

import java.util.UUID;

/** Published after a new organization is successfully persisted. */
public record OrganizationCreatedEvent(UUID organizationId) {}
