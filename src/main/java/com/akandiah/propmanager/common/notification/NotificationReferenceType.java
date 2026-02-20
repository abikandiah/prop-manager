package com.akandiah.propmanager.common.notification;

/**
 * Discriminator for the domain entity a {@code NotificationDelivery} relates to.
 * Stored as a string in the {@code notification_deliveries.reference_type} column.
 */
public enum NotificationReferenceType {
	INVITE,
	LEASE,
	USER
}
