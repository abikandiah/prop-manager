package com.akandiah.propmanager.features.tenant.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.akandiah.propmanager.features.lease.domain.LateFeeType;
import com.akandiah.propmanager.features.lease.domain.Lease;
import com.akandiah.propmanager.features.lease.domain.LeaseStatus;

public record TenantLeaseSummary(
		UUID id,
		LeaseStatus status,
		LocalDate startDate,
		LocalDate endDate,
		BigDecimal rentAmount,
		Integer rentDueDay,
		BigDecimal securityDepositHeld,
		LateFeeType lateFeeType,
		BigDecimal lateFeeAmount,
		Integer noticePeriodDays,
		LocalDate noticeDeadlineDate,
		String leaseTemplateName,
		String leaseTemplateVersionTag,
		String executedContentMarkdown) {

	public static TenantLeaseSummary from(Lease lease) {
		LocalDate noticeDeadline = null;
		if (lease.getNoticePeriodDays() != null && lease.getEndDate() != null) {
			noticeDeadline = lease.getEndDate().minusDays(lease.getNoticePeriodDays());
		}
		return new TenantLeaseSummary(
				lease.getId(),
				lease.getStatus(),
				lease.getStartDate(),
				lease.getEndDate(),
				lease.getRentAmount(),
				lease.getRentDueDay(),
				lease.getSecurityDepositHeld(),
				lease.getLateFeeType(),
				lease.getLateFeeAmount(),
				lease.getNoticePeriodDays(),
				noticeDeadline,
				lease.getLeaseTemplateName(),
				lease.getLeaseTemplateVersionTag(),
				lease.getExecutedContentMarkdown());
	}
}
