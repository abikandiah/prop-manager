package com.akandiah.propmanager.features.lease.service;

import org.springframework.stereotype.Component;

import com.akandiah.propmanager.features.lease.api.dto.CreateLeaseRequest;
import com.akandiah.propmanager.features.prop.domain.Prop;
import com.akandiah.propmanager.features.unit.domain.Unit;

/**
 * Utility for rendering lease template markdown with placeholder substitution.
 */
@Component
public class LeaseTemplateRenderer {

	/**
	 * Simple placeholder stamping. Replaces {{key}} tokens in the template
	 * markdown with concrete lease values.
	 */
	public String stampMarkdown(String markdown, CreateLeaseRequest req, Unit unit, Prop property) {
		if (markdown == null) {
			return null;
		}
		return markdown
				.replace("{{property_name}}", property.getLegalName())
				.replace("{{unit_number}}", unit.getUnitNumber())
				.replace("{{start_date}}", req.startDate().toString())
				.replace("{{end_date}}", req.endDate().toString())
				.replace("{{rent_amount}}", req.rentAmount().toPlainString())
				.replace("{{rent_due_day}}", req.rentDueDay().toString())
				.replace("{{security_deposit}}", req.securityDepositHeld() != null
						? req.securityDepositHeld().toPlainString()
						: "N/A");
	}

	public static <T> T coalesce(T override, T fallback) {
		return override != null ? override : fallback;
	}
}
