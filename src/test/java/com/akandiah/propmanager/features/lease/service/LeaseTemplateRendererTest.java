package com.akandiah.propmanager.features.lease.service;

import static com.akandiah.propmanager.TestDataFactory.lease;
import static com.akandiah.propmanager.TestDataFactory.prop;
import static com.akandiah.propmanager.TestDataFactory.unit;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.akandiah.propmanager.features.lease.domain.Lease;
import com.akandiah.propmanager.features.prop.domain.Prop;
import com.akandiah.propmanager.features.unit.domain.Unit;

/**
 * Unit tests for {@link LeaseTemplateRenderer}.
 * Tests template markdown rendering and placeholder substitution.
 */
class LeaseTemplateRendererTest {

	private LeaseTemplateRenderer renderer;

	@BeforeEach
	void setUp() {
		renderer = new LeaseTemplateRenderer();
	}

	@Test
	void shouldRenderTemplateWithBuiltInPlaceholders() {
		Prop property = prop()
				.id(UUID.randomUUID())
				.legalName("Sunset Towers")
				.build();

		Unit leaseUnit = unit()
				.id(UUID.randomUUID())
				.prop(property)
				.unitNumber("204")
				.build();

		Lease lease = lease()
				.property(property)
				.unit(leaseUnit)
				.startDate(LocalDate.of(2026, 3, 1))
				.endDate(LocalDate.of(2027, 2, 28))
				.rentAmount(new BigDecimal("2500.00"))
				.rentDueDay(5)
				.securityDepositHeld(new BigDecimal("2500.00"))
				.build();

		String template = "Property: {{property_name}}\n"
				+ "Unit: {{unit_number}}\n"
				+ "Rent: ${{rent_amount}} due on day {{rent_due_day}}\n"
				+ "Period: {{start_date}} to {{end_date}}\n"
				+ "Deposit: ${{security_deposit}}";

		String result = renderer.stampMarkdownFromLease(template, lease, leaseUnit, property, null);

		assertThat(result).contains("Property: Sunset Towers");
		assertThat(result).contains("Unit: 204");
		assertThat(result).contains("Rent: $2500.00 due on day 5");
		assertThat(result).contains("Period: 2026-03-01 to 2027-02-28");
		assertThat(result).contains("Deposit: $2500.00");
	}

	@Test
	void shouldMergeTemplateDefaultParameters() {
		Prop property = prop().id(UUID.randomUUID()).build();
		Unit leaseUnit = unit().id(UUID.randomUUID()).build();
		Lease lease = lease().property(property).unit(leaseUnit).build();

		Map<String, String> templateDefaults = Map.of(
				"landlord_name", "John Smith Properties",
				"tenant_name", "Default Tenant");

		String template = "Landlord: {{landlord_name}}\nTenant: {{tenant_name}}";

		String result = renderer.stampMarkdownFromLease(template, lease, leaseUnit, property, templateDefaults);

		assertThat(result).contains("Landlord: John Smith Properties");
		assertThat(result).contains("Tenant: Default Tenant");
	}

	@Test
	void shouldAllowLeaseParametersToOverrideTemplateDefaults() {
		Map<String, String> templateDefaults = new HashMap<>();
		templateDefaults.put("landlord_name", "Default Landlord");
		templateDefaults.put("special_clause", "Standard clause");

		Map<String, String> leaseOverrides = new HashMap<>();
		leaseOverrides.put("special_clause", "Custom clause for this lease");

		Prop property = prop().id(UUID.randomUUID()).build();
		Unit leaseUnit = unit().id(UUID.randomUUID()).build();
		Lease lease = lease()
				.property(property)
				.unit(leaseUnit)
				.templateParameters(leaseOverrides)
				.build();

		String template = "Landlord: {{landlord_name}}\nClause: {{special_clause}}";

		String result = renderer.stampMarkdownFromLease(template, lease, leaseUnit, property, templateDefaults);

		assertThat(result).contains("Landlord: Default Landlord");
		assertThat(result).contains("Clause: Custom clause for this lease");
	}

	@Test
	void shouldHandleNullSecurityDeposit() {
		Prop property = prop().id(UUID.randomUUID()).build();
		Unit leaseUnit = unit().id(UUID.randomUUID()).build();
		Lease lease = lease()
				.property(property)
				.unit(leaseUnit)
				.securityDepositHeld(null)
				.build();

		String template = "Security Deposit: {{security_deposit}}";

		String result = renderer.stampMarkdownFromLease(template, lease, leaseUnit, property, null);

		assertThat(result).contains("Security Deposit: N/A");
	}

	@Test
	void shouldHandleNullMarkdown() {
		Prop property = prop().id(UUID.randomUUID()).build();
		Unit leaseUnit = unit().id(UUID.randomUUID()).build();
		Lease lease = lease().property(property).unit(leaseUnit).build();

		String result = renderer.stampMarkdownFromLease(null, lease, leaseUnit, property, null);

		assertThat(result).isNull();
	}

	@Test
	void shouldReplaceAllOccurrencesOfPlaceholder() {
		Prop property = prop().id(UUID.randomUUID()).build();
		Unit leaseUnit = unit().id(UUID.randomUUID()).build();
		Lease lease = lease()
				.property(property)
				.unit(leaseUnit)
				.rentAmount(new BigDecimal("1800.00"))
				.build();

		String template = "Monthly rent: ${{rent_amount}}\nFirst payment: ${{rent_amount}}\nLast payment: ${{rent_amount}}";

		String result = renderer.stampMarkdownFromLease(template, lease, leaseUnit, property, null);

		assertThat(result).isEqualTo("Monthly rent: $1800.00\nFirst payment: $1800.00\nLast payment: $1800.00");
	}

	@Test
	void shouldHandleEmptyTemplateParameters() {
		Prop property = prop().id(UUID.randomUUID()).legalName("Test Property").build();
		Unit leaseUnit = unit().id(UUID.randomUUID()).unitNumber("A1").build();
		Lease lease = lease()
				.property(property)
				.unit(leaseUnit)
				.templateParameters(new HashMap<>())
				.build();

		String template = "Property: {{property_name}}, Unit: {{unit_number}}";

		String result = renderer.stampMarkdownFromLease(template, lease, leaseUnit, property, new HashMap<>());

		assertThat(result).isEqualTo("Property: Test Property, Unit: A1");
	}

	@Test
	void shouldLeaveUnknownPlaceholdersUnchanged() {
		Prop property = prop().id(UUID.randomUUID()).legalName("Sunrise Apartments").build();
		Unit leaseUnit = unit().id(UUID.randomUUID()).build();
		Lease lease = lease().property(property).unit(leaseUnit).build();

		String template = "Known: {{property_name}}\nUnknown: {{unknown_placeholder}}";

		String result = renderer.stampMarkdownFromLease(template, lease, leaseUnit, property, null);

		assertThat(result).contains("Known: Sunrise Apartments");
		assertThat(result).contains("Unknown: {{unknown_placeholder}}");
	}

	@Test
	void shouldCoalesceNullToFallback() {
		String result = LeaseTemplateRenderer.coalesce(null, "fallback");
		assertThat(result).isEqualTo("fallback");
	}

	@Test
	void shouldCoalesceNonNullToOverride() {
		String result = LeaseTemplateRenderer.coalesce("override", "fallback");
		assertThat(result).isEqualTo("override");
	}

	@Test
	void shouldCoalesceWithNullFallback() {
		String result = LeaseTemplateRenderer.coalesce(null, null);
		assertThat(result).isNull();
	}

	@Test
	void shouldCoalesceWithNumberTypes() {
		Integer result = LeaseTemplateRenderer.coalesce(100, 50);
		assertThat(result).isEqualTo(100);

		result = LeaseTemplateRenderer.coalesce(null, 50);
		assertThat(result).isEqualTo(50);
	}

	@Test
	void shouldHandleNullParameterValues() {
		Map<String, String> params = new HashMap<>();
		params.put("nullable_field", null);

		Prop property = prop().id(UUID.randomUUID()).build();
		Unit leaseUnit = unit().id(UUID.randomUUID()).build();
		Lease lease = lease()
				.property(property)
				.unit(leaseUnit)
				.templateParameters(params)
				.build();

		String template = "Field: {{nullable_field}}";

		String result = renderer.stampMarkdownFromLease(template, lease, leaseUnit, property, null);

		// Null values should be replaced with empty string
		assertThat(result).isEqualTo("Field: ");
	}
}
