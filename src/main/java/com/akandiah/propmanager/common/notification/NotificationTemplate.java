package com.akandiah.propmanager.common.notification;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Email notification templates used across the application.
 * Each template defines a subject and the path to its HTML/text template file.
 */
@Getter
@RequiredArgsConstructor
public enum NotificationTemplate {

	// Invite templates
	INVITE_LEASE("Invitation to Join Lease", "invite-lease"),
	INVITE_PROPERTY("Invitation to Manage Property", "invite-property"),

	// Lease lifecycle templates
	LEASE_CREATED("Lease Agreement Ready for Review", "lease-created"),
	LEASE_SIGNED("Lease Agreement Signed", "lease-signed"),
	LEASE_EXPIRING_SOON("Lease Expiring Soon", "lease-expiring"),

	// Tenant notifications
	PAYMENT_REMINDER("Payment Due Reminder", "payment-reminder"),
	PAYMENT_RECEIVED("Payment Received", "payment-received"),

	// Maintenance
	MAINTENANCE_REQUEST_CREATED("Maintenance Request Submitted", "maintenance-created"),
	MAINTENANCE_REQUEST_UPDATE("Maintenance Request Update", "maintenance-update"),

	// Account/Auth
	PASSWORD_RESET("Password Reset Request", "password-reset"),
	ACCOUNT_CREATED("Welcome to PropMange", "account-created");

	private final String subject;
	private final String templateName;

	/**
	 * Get the full template path for this template.
	 * Templates are stored in src/main/resources/templates/email/
	 */
	public String getTemplatePath() {
		return "email/" + templateName;
	}
}
