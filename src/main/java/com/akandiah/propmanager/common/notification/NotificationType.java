package com.akandiah.propmanager.common.notification;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * All notification types in the system.
 * Maps each type to its template and whether users can opt out.
 */
@Getter
@RequiredArgsConstructor
public enum NotificationType {

	INVITE_LEASE(NotificationTemplate.INVITE_LEASE, false),
	INVITE_PROPERTY(NotificationTemplate.INVITE_PROPERTY, false),
	ACCOUNT_CREATED(NotificationTemplate.ACCOUNT_CREATED, false),
	LEASE_SUBMITTED_FOR_REVIEW(NotificationTemplate.LEASE_CREATED, true),
	LEASE_ACTIVATED(NotificationTemplate.LEASE_SIGNED, true),
	LEASE_EXPIRING_SOON(NotificationTemplate.LEASE_EXPIRING_SOON, true);

	private final NotificationTemplate template;
	private final boolean optOutAllowed;
}
