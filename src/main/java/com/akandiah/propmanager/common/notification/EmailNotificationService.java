package com.akandiah.propmanager.common.notification;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for sending email notifications using JavaMail and Thymeleaf templates.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailNotificationService {

	private final JavaMailSender mailSender;
	private final TemplateEngine templateEngine;

	@Value("${spring.mail.from:noreply@propmanager.local}")
	private String fromEmail;

	@Value("${app.notification.email.enabled:true}")
	private boolean emailEnabled;

	/**
	 * Send an email using the specified template and context data.
	 *
	 * @param toEmail  Recipient email address
	 * @param template Template to use
	 * @param context  Data to populate the template
	 * @throws NotificationException if sending fails
	 */
	public void send(String toEmail, NotificationTemplate template, Map<String, Object> context) {
		if (!emailEnabled) {
			log.info("Email notifications disabled. Skipping email to {}", toEmail);
			return;
		}

		try {
			String htmlContent = renderTemplate(template, context);
			sendEmail(toEmail, template.getSubject(), htmlContent);
			log.info("Email sent successfully: template={}, to={}", template.name(), toEmail);
		} catch (Exception e) {
			log.error("Failed to send email: template={}, to={}", template.name(), toEmail, e);
			throw new NotificationException("Failed to send email to " + toEmail, e);
		}
	}

	/**
	 * Render the email template with the provided context data.
	 */
	private String renderTemplate(NotificationTemplate template, Map<String, Object> context) {
		try {
			Context thymeleafContext = new Context();
			thymeleafContext.setVariables(context);

			// Add common variables available to all templates
			thymeleafContext.setVariable("appName", "Prop Manager");

			return templateEngine.process(template.getTemplatePath(), thymeleafContext);
		} catch (Exception e) {
			log.error("Failed to render template: {}", template.getTemplatePath(), e);
			throw new NotificationException("Failed to render email template: " + template.name(), e);
		}
	}

	/**
	 * Send the actual email via JavaMailSender.
	 */
	private void sendEmail(String toEmail, String subject, String htmlContent) throws MessagingException {
		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
				StandardCharsets.UTF_8.name());

		helper.setFrom(fromEmail);
		helper.setTo(toEmail);
		helper.setSubject(subject);
		helper.setText(htmlContent, true); // true = HTML

		mailSender.send(message);
	}
}
