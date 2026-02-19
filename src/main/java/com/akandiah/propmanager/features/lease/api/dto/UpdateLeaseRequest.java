package com.akandiah.propmanager.features.lease.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import com.akandiah.propmanager.features.lease.domain.LateFeeType;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Partial update for a DRAFT lease.
 * Only DRAFT leases may be modified; all other statuses are read-only.
 * Tenants are managed separately via /api/leases/{id}/tenants.
 */
public record UpdateLeaseRequest(
		LocalDate startDate,

		LocalDate endDate,

		@PositiveOrZero BigDecimal rentAmount,

		@Min(1) @Max(28) Integer rentDueDay,

		@PositiveOrZero BigDecimal securityDepositHeld,

		LateFeeType lateFeeType,

		@PositiveOrZero BigDecimal lateFeeAmount,

		@Min(1) Integer noticePeriodDays,

		/** Allow the owner to tweak the stamped content before sending for review. */
		String executedContentMarkdown,

		Map<String, Object> additionalMetadata,

		/** Extra placeholders for template: {{key}} → value. */
		Map<String, String> templateParameters,

		@NotNull(message = "version is required for optimistic-lock verification") Integer version) {

	@AssertTrue(message = "Start date must be before end date")
	public boolean isDateRangeValid() {
		if (startDate == null || endDate == null) return true;
		return startDate.isBefore(endDate);
	}
}
