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
	INVITE_LEASE("You've Been Invited to Join a Lease", "invite-lease"),
	INVITE_ORG("You've Been Invited to Join an Organization", "invite-org"),

	// Lease lifecycle templates
	LEASE_CREATED("Lease Agreement Ready for Review", "lease-created"),
	LEASE_SIGNED("Your Lease Is Now Active", "lease-signed"),
	LEASE_EXPIRING_SOON("Lease Expiring Soon", "lease-expiring"),
	LEASE_TENANT_SIGNED("Tenant Has Signed the Lease", "lease-tenant-signed"),
	LEASE_ALL_SIGNED("All Tenants Have Signed — Ready to Activate", "lease-all-signed"),
	LEASE_EXPIRED("Lease Has Expired", "lease-expired"),
	LEASE_TERMINATED("Lease Has Been Terminated", "lease-terminated"),
	LEASE_EVICTED("Eviction Notice", "lease-evicted"),

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
