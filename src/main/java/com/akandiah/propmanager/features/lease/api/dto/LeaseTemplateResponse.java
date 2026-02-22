package com.akandiah.propmanager.features.lease.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.akandiah.propmanager.features.lease.domain.LateFeeType;
import com.akandiah.propmanager.features.lease.domain.LeaseTemplate;

public record LeaseTemplateResponse(
		UUID id,
		String name,
		String versionTag,
		Integer version,
		String templateMarkdown,
		LateFeeType defaultLateFeeType,
		BigDecimal defaultLateFeeAmount,
		Integer defaultNoticePeriodDays,
		boolean active,
		Map<String, String> templateParameters,
		Instant createdAt,
		Instant updatedAt) {

	public static LeaseTemplateResponse from(LeaseTemplate t) {
		return new LeaseTemplateResponse(
				t.getId(),
				t.getName(),
				t.getVersionTag(),
				t.getVersion(),
				t.getTemplateMarkdown(),
				t.getDefaultLateFeeType(),
				t.getDefaultLateFeeAmount(),
				t.getDefaultNoticePeriodDays(),
				t.getActive(),
				t.getTemplateParameters(),
				t.getCreatedAt(),
				t.getUpdatedAt());
	}
}
