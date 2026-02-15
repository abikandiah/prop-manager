package com.akandiah.propmanager.features.lease.service;

import static com.akandiah.propmanager.TestDataFactory.lease;
import static com.akandiah.propmanager.TestDataFactory.leaseTemplate;
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

import com.akandiah.propmanager.features.lease.api.dto.CreateLeaseRequest;
import com.akandiah.propmanager.features.lease.domain.LeaseTemplate;
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

		CreateLeaseRequest request = lease()
				.property(property)
				.unit(leaseUnit)
				.startDate(LocalDate.of(2026, 3, 1))
				.endDate(LocalDate.of(2027, 2, 28))
				.rentAmount(new BigDecimal("2500.00"))
				.rentDueDay(5)
				.securityDepositHeld(new BigDecimal("2500.00"))
				.buildCreateRequest();

		String template = "Property: {{property_name}}\n"
				+ "Unit: {{unit_number}}\n"
				+ "Rent: ${{rent_amount}} due on day {{rent_due_day}}\n"
				+ "Period: {{start_date}} to {{end_date}}\n"
				+ "Deposit: ${{security_deposit}}";

		String result = renderer.stampMarkdown(template, request, leaseUnit, property, null);

		assertThat(result).contains("Property: Sunset Towers");
		assertThat(result).contains("Unit: 204");
		assertThat(result).contains("Rent: $2500.00 due on day 5");
		assertThat(result).contains("Period: 2026-03-01 to 2027-02-28");
		assertThat(result).contains("Deposit: $2500.00");
	}

	@Test
	void shouldMergeTemplateDefaultParameters() {
		LeaseTemplate template = leaseTemplate()
				.templateMarkdown("Landlord: {{landlord_name}}\nTenant: {{tenant_name}}")
				.templateParameters(Map.of(
						"landlord_name", "John Smith Properties",
						"tenant_name", "Default Tenant"))
				.build();

		CreateLeaseRequest request = lease().buildCreateRequest();
		Prop property = prop().id(UUID.randomUUID()).build();
		Unit leaseUnit = unit().id(UUID.randomUUID()).build();

		String result = renderer.stampMarkdown(
				template.getTemplateMarkdown(),
				request,
				leaseUnit,
				property,
				template.getTemplateParameters());

		assertThat(result).contains("Landlord: John Smith Properties");
		assertThat(result).contains("Tenant: Default Tenant");
	}

	@Test
	void shouldAllowRequestParametersToOverrideTemplateDefaults() {
		Map<String, String> templateDefaults = new HashMap<>();
		templateDefaults.put("landlord_name", "Default Landlord");
		templateDefaults.put("special_clause", "Standard clause");

		Map<String, String> requestOverrides = new HashMap<>();
		requestOverrides.put("special_clause", "Custom clause for this lease");

		CreateLeaseRequest request = lease()
				.templateParameters(requestOverrides)
				.buildCreateRequest();

		Prop property = prop().id(UUID.randomUUID()).build();
		Unit leaseUnit = unit().id(UUID.randomUUID()).build();

		String template = "Landlord: {{landlord_name}}\nClause: {{special_clause}}";

		String result = renderer.stampMarkdown(template, request, leaseUnit, property, templateDefaults);

		assertThat(result).contains("Landlord: Default Landlord");
		assertThat(result).contains("Clause: Custom clause for this lease");
	}

	@Test
	void shouldHandleNullSecurityDeposit() {
		CreateLeaseRequest request = lease()
				.securityDepositHeld(null)
				.buildCreateRequest();

		Prop property = prop().id(UUID.randomUUID()).build();
		Unit leaseUnit = unit().id(UUID.randomUUID()).build();

		String template = "Security Deposit: {{security_deposit}}";

		String result = renderer.stampMarkdown(template, request, leaseUnit, property, null);

		assertThat(result).contains("Security Deposit: N/A");
	}

	@Test
	void shouldHandleNullMarkdown() {
		CreateLeaseRequest request = lease().buildCreateRequest();
		Prop property = prop().id(UUID.randomUUID()).build();
		Unit leaseUnit = unit().id(UUID.randomUUID()).build();

		String result = renderer.stampMarkdown(null, request, leaseUnit, property, null);

		assertThat(result).isNull();
	}

	@Test
	void shouldReplaceAllOccurrencesOfPlaceholder() {
		CreateLeaseRequest request = lease()
				.rentAmount(new BigDecimal("1800.00"))
				.buildCreateRequest();

		Prop property = prop().id(UUID.randomUUID()).build();
		Unit leaseUnit = unit().id(UUID.randomUUID()).build();

		String template = "Monthly rent: ${{rent_amount}}\nFirst payment: ${{rent_amount}}\nLast payment: ${{rent_amount}}";

		String result = renderer.stampMarkdown(template, request, leaseUnit, property, null);

		assertThat(result).isEqualTo("Monthly rent: $1800.00\nFirst payment: $1800.00\nLast payment: $1800.00");
	}

	@Test
	void shouldHandleEmptyTemplateParameters() {
		CreateLeaseRequest request = lease()
				.templateParameters(new HashMap<>())
				.buildCreateRequest();

		Prop property = prop().id(UUID.randomUUID()).legalName("Test Property").build();
		Unit leaseUnit = unit().id(UUID.randomUUID()).unitNumber("A1").build();

		String template = "Property: {{property_name}}, Unit: {{unit_number}}";

		String result = renderer.stampMarkdown(template, request, leaseUnit, property, new HashMap<>());

		assertThat(result).isEqualTo("Property: Test Property, Unit: A1");
	}

	@Test
	void shouldLeaveUnknownPlaceholdersUnchanged() {
		CreateLeaseRequest request = lease().buildCreateRequest();
		Prop property = prop().id(UUID.randomUUID()).build();
		Unit leaseUnit = unit().id(UUID.randomUUID()).build();

		String template = "Known: {{property_name}}\nUnknown: {{unknown_placeholder}}";

		String result = renderer.stampMarkdown(template, request, leaseUnit, property, null);

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

		CreateLeaseRequest request = lease()
				.templateParameters(params)
				.buildCreateRequest();

		Prop property = prop().id(UUID.randomUUID()).build();
		Unit leaseUnit = unit().id(UUID.randomUUID()).build();

		String template = "Field: {{nullable_field}}";

		String result = renderer.stampMarkdown(template, request, leaseUnit, property, null);

		// Null values should be replaced with empty string
		assertThat(result).isEqualTo("Field: ");
	}
}
