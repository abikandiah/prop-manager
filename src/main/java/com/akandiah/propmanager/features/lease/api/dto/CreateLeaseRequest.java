package com.akandiah.propmanager.features.lease.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.akandiah.propmanager.features.lease.domain.LateFeeType;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Stamps a new lease from a template.
 * The template's defaults are used for any field left null.
 */
public record CreateLeaseRequest(
		@NotNull(message = "Lease template ID is required") UUID leaseTemplateId,

		@NotNull(message = "Unit ID is required") UUID unitId,

		@NotNull(message = "Property ID is required") UUID propertyId,

		List<@Email String> tenantEmails,

		@NotNull(message = "Start date is required") LocalDate startDate,

		@NotNull(message = "End date is required") LocalDate endDate,

		@NotNull(message = "Rent amount is required") @PositiveOrZero BigDecimal rentAmount,

		@NotNull(message = "Rent due day is required") @Min(1) @Max(28) Integer rentDueDay,

		@PositiveOrZero BigDecimal securityDepositHeld,

		/** Overrides the template default if provided. */
		LateFeeType lateFeeType,

		@PositiveOrZero BigDecimal lateFeeAmount,

		@Min(1) Integer noticePeriodDays,

		Map<String, Object> additionalMetadata,

		/** Extra placeholders for template: {{key}} → value. Can add or override built-in params. */
		Map<String, String> templateParameters) {

	@AssertTrue(message = "Start date must be before end date")
	public boolean isDateRangeValid() {
		return startDate == null || endDate == null || startDate.isBefore(endDate);
	}
}
