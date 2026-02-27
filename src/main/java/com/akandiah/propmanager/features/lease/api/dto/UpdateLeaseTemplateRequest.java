package com.akandiah.propmanager.features.lease.api.dto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import com.akandiah.propmanager.features.lease.domain.LateFeeType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateLeaseTemplateRequest(
		@NotNull(message = "orgId is required") UUID orgId,

		@Size(max = 255) String name,

		@Size(max = 50) String versionTag,

		String templateMarkdown,

		LateFeeType defaultLateFeeType,

		@PositiveOrZero(message = "Late fee amount must be zero or positive") BigDecimal defaultLateFeeAmount,

		@Min(value = 1, message = "Notice period must be at least 1 day") Integer defaultNoticePeriodDays,

		/** Whether this template can be used for new leases (enable/disable). */
		Boolean active,

		/** Default placeholder values for {{key}} in the template markdown. */
		Map<String, String> templateParameters,

		/**
		 * Required for optimistic-lock verification.
		 * Must match the current version on the server; if stale,
		 * the update is rejected with 409 Conflict.
		 */
		@NotNull(message = "version is required for optimistic-lock verification") Integer version) {
}
