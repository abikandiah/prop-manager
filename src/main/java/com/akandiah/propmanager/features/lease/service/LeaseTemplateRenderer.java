package com.akandiah.propmanager.features.lease.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.akandiah.propmanager.features.lease.api.dto.CreateLeaseRequest;
import com.akandiah.propmanager.features.lease.domain.Lease;
import com.akandiah.propmanager.features.prop.domain.Prop;
import com.akandiah.propmanager.features.unit.domain.Unit;

/**
 * Utility for rendering lease template markdown with placeholder substitution.
 * Merge order: built-in params, then template default templateParameters,
 * then request templateParameters (so per-lease overrides win).
 */
@Component
public class LeaseTemplateRenderer {

	/**
	 * Stamps the template markdown. Parameter merge order: built-in (from
	 * request/unit/property), then template's default templateParameters, then
	 * request's templateParameters.
	 */
	public String stampMarkdown(String markdown, CreateLeaseRequest req, Unit unit, Prop property,
			Map<String, String> templateDefaultParameters) {
		if (markdown == null) {
			return null;
		}
		Map<String, String> params = buildParameterMap(req, unit, property);
		if (templateDefaultParameters != null) {
			params.putAll(templateDefaultParameters);
		}
		if (req.templateParameters() != null) {
			params.putAll(req.templateParameters());
		}
		return substitutePlaceholders(markdown, params);
	}

	/**
	 * Stamps the template markdown using data from an existing lease (e.g. on activate).
	 * Use when the lease is already persisted and we are freezing the template content.
	 */
	public String stampMarkdownFromLease(String markdown, Lease lease, Unit unit, Prop property,
			Map<String, String> templateDefaultParameters) {
		if (markdown == null) {
			return null;
		}
		Map<String, String> params = buildParameterMapFromLease(lease, unit, property);
		if (templateDefaultParameters != null) {
			params.putAll(templateDefaultParameters);
		}
		if (lease.getTemplateParameters() != null) {
			params.putAll(lease.getTemplateParameters());
		}
		return substitutePlaceholders(markdown, params);
	}

	private Map<String, String> buildParameterMap(CreateLeaseRequest req, Unit unit, Prop property) {
		Map<String, String> params = new LinkedHashMap<>();
		params.put("property_name", property.getLegalName());
		params.put("unit_number", unit.getUnitNumber());
		params.put("start_date", req.startDate().toString());
		params.put("end_date", req.endDate().toString());
		params.put("rent_amount", req.rentAmount().toPlainString());
		params.put("rent_due_day", req.rentDueDay().toString());
		params.put("security_deposit", req.securityDepositHeld() != null
				? req.securityDepositHeld().toPlainString()
				: "N/A");
		return params;
	}

	private Map<String, String> buildParameterMapFromLease(Lease lease, Unit unit, Prop property) {
		Map<String, String> params = new LinkedHashMap<>();
		params.put("property_name", property.getLegalName());
		params.put("unit_number", unit.getUnitNumber());
		params.put("start_date", lease.getStartDate().toString());
		params.put("end_date", lease.getEndDate().toString());
		params.put("rent_amount", lease.getRentAmount().toPlainString());
		params.put("rent_due_day", lease.getRentDueDay().toString());
		params.put("security_deposit", lease.getSecurityDepositHeld() != null
				? lease.getSecurityDepositHeld().toPlainString()
				: "N/A");
		return params;
	}

	private String substitutePlaceholders(String markdown, Map<String, String> params) {
		String result = markdown;
		for (Map.Entry<String, String> e : params.entrySet()) {
			String key = e.getKey();
			String value = e.getValue() != null ? e.getValue() : "";
			result = result.replace("{{" + key + "}}", value);
		}
		return result;
	}

	public static <T> T coalesce(T override, T fallback) {
		return override != null ? override : fallback;
	}
}
