package com.akandiah.propmanager.features.lease.api.dto;

import java.math.BigDecimal;
import java.util.Map;

import com.akandiah.propmanager.features.lease.domain.LateFeeType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateLeaseTemplateRequest(
		@NotBlank(message = "Name is required") @Size(max = 255) String name,

		@Size(max = 50) String versionTag,

		@NotBlank(message = "Template markdown is required") String templateMarkdown,

		LateFeeType defaultLateFeeType,

		@PositiveOrZero(message = "Late fee amount must be zero or positive") BigDecimal defaultLateFeeAmount,

		@Min(value = 1, message = "Notice period must be at least 1 day") Integer defaultNoticePeriodDays,

		/** Default placeholder values for {{key}} in the template markdown. */
		Map<String, String> templateParameters) {
}
