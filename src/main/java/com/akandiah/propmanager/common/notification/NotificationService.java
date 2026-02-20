package com.akandiah.propmanager.common.notification;

import java.util.Map;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Facade for sending notifications across the application.
 * Currently supports email; can be extended for SMS, push notifications, etc.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

	private final EmailNotificationService emailNotificationService;

	/**
	 * Send a notification email synchronously.
	 *
	 * @param toEmail  Recipient email address
	 * @param template Template to use
	 * @param context  Data to populate the template
	 * @throws NotificationException if sending fails
	 */
	public void send(String toEmail, NotificationTemplate template, Map<String, Object> context) {
		log.info("Sending notification: template={}, to={}", template.name(), toEmail);
		emailNotificationService.send(toEmail, template, context);
	}

	/**
	 * Send a notification to multiple recipients.
	 *
	 * @param toEmails List of recipient email addresses
	 * @param template Template to use
	 * @param context  Data to populate the template
	 */
	public void sendToMultiple(Iterable<String> toEmails, NotificationTemplate template,
			Map<String, Object> context) {
		for (String email : toEmails) {
			try {
				send(email, template, context);
			} catch (Exception e) {
				log.error("Failed to send notification to {}: template={}", email, template.name(), e);
				// Continue sending to other recipients
			}
		}
	}
}
