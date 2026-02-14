package com.akandiah.propmanager.features.lease.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import com.akandiah.propmanager.features.lease.domain.LateFeeType;
import com.akandiah.propmanager.features.lease.domain.Lease;
import com.akandiah.propmanager.features.lease.domain.LeaseStatus;

public record LeaseResponse(
		UUID id,
		UUID leaseTemplateId,
		String leaseTemplateName,
		String leaseTemplateVersionTag,
		UUID unitId,
		UUID propertyId,
		LeaseStatus status,
		Integer version,
		LocalDate startDate,
		LocalDate endDate,
		BigDecimal rentAmount,
		String executedContentMarkdown,
		Integer rentDueDay,
		BigDecimal securityDepositHeld,
		LateFeeType lateFeeType,
		BigDecimal lateFeeAmount,
		Integer noticePeriodDays,
		Map<String, Object> additionalMetadata,
		Map<String, String> templateParameters,
		Instant createdAt,
		Instant updatedAt) {

	public static LeaseResponse from(Lease l) {
		return new LeaseResponse(
				l.getId(),
				l.getLeaseTemplate() != null ? l.getLeaseTemplate().getId() : null,
				l.getLeaseTemplateName(),
				l.getLeaseTemplateVersionTag(),
				l.getUnit() != null ? l.getUnit().getId() : null,
				l.getProperty() != null ? l.getProperty().getId() : null,
				l.getStatus(),
				l.getVersion(),
				l.getStartDate(),
				l.getEndDate(),
				l.getRentAmount(),
				l.getExecutedContentMarkdown(),
				l.getRentDueDay(),
				l.getSecurityDepositHeld(),
				l.getLateFeeType(),
				l.getLateFeeAmount(),
				l.getNoticePeriodDays(),
				l.getAdditionalMetadata(),
				l.getTemplateParameters(),
				l.getCreatedAt(),
				l.getUpdatedAt());
	}
}
