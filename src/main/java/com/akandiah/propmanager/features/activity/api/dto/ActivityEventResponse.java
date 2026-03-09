package com.akandiah.propmanager.features.activity.api.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.akandiah.propmanager.features.activity.domain.ActivityEvent;
import com.akandiah.propmanager.features.activity.domain.ActivityEventType;
import com.akandiah.propmanager.features.activity.domain.ActivitySubjectType;

public record ActivityEventResponse(
		UUID id,
		UUID orgId,
		ActivityEventType eventType,
		ActivitySubjectType subjectType,
		UUID subjectId,
		UUID actorId,
		Map<String, Object> metadata,
		Integer version,
		Instant createdAt,
		Instant updatedAt,
		String description
) {
	public static ActivityEventResponse from(ActivityEvent event) {
		return new ActivityEventResponse(
				event.getId(),
				event.getOrgId(),
				event.getEventType(),
				event.getSubjectType(),
				event.getSubjectId(),
				event.getActorId(),
				event.getMetadata(),
				event.getVersion(),
				event.getCreatedAt(),
				event.getUpdatedAt(),
				describe(event)
		);
	}

	private static String describe(ActivityEvent event) {
		String base = switch (event.getEventType()) {
			case LEASE_SUBMITTED_FOR_REVIEW -> "Lease submitted for review";
			case LEASE_TENANT_SIGNED -> "Tenant signed lease";
			case LEASE_ALL_TENANTS_SIGNED -> "All tenants signed lease";
			case LEASE_ACTIVATED -> "Lease activated";
			case LEASE_EXPIRING_SOON -> "Lease expiring soon";
			case LEASE_EXPIRED -> "Lease expired";
			case LEASE_TERMINATED -> "Lease terminated";
			case LEASE_EVICTED -> "Tenant evicted";
			case ORGANIZATION_CREATED -> "Organization created";
			case INVITE_ACCEPTED -> "Invite accepted";
		};
		String context = buildContext(event.getMetadata());
		return context.isEmpty() ? base : base + ": " + context;
	}

	/** Builds "Unit 101 (Sunset Heights)" from metadata, or empty string if unavailable. */
	private static String buildContext(Map<String, Object> metadata) {
		if (metadata == null) return "";
		String unitNumber = metadata.get("unitNumber") instanceof String s ? s : null;
		String propertyName = metadata.get("propertyName") instanceof String s ? s : null;
		if (unitNumber != null && propertyName != null) return "Unit " + unitNumber + " (" + propertyName + ")";
		if (unitNumber != null) return "Unit " + unitNumber;
		if (propertyName != null) return propertyName;
		return "";
	}
}
