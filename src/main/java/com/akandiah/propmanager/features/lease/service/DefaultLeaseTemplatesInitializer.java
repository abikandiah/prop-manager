package com.akandiah.propmanager.features.lease.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.features.lease.domain.LateFeeType;
import com.akandiah.propmanager.features.lease.domain.LeaseTemplate;
import com.akandiah.propmanager.features.lease.domain.LeaseTemplateRepository;

/**
 * Initializes default lease templates on first application startup.
 * Idempotent - only creates templates if none exist.
 * Reads template markdown from classpath resources for easier maintenance.
 */
@Component
public class DefaultLeaseTemplatesInitializer implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DefaultLeaseTemplatesInitializer.class);
	private static final String TEMPLATE_BASE_PATH = "classpath:lease-templates/";

	private final LeaseTemplateRepository repository;
	private final ResourceLoader resourceLoader;

	public DefaultLeaseTemplatesInitializer(LeaseTemplateRepository repository, ResourceLoader resourceLoader) {
		this.repository = repository;
		this.resourceLoader = resourceLoader;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		long count = repository.count();
		if (count > 0) {
			log.info("[Data Init] Found {} existing lease template(s), skipping defaults", count);
			return;
		}

		log.info("[Data Init] No lease templates found. Creating default templates...");

		createResidentialLeaseTemplate();
		createCommercialLeaseTemplate();

		log.info("[Data Init] Default lease templates created successfully");
	}

	private void createResidentialLeaseTemplate() {
		String markdown = loadMarkdownFromResources("residential-lease-template.md");

		LeaseTemplate residential = LeaseTemplate.builder()
				.name("Standard Residential Lease Agreement")
				.versionTag("v1.0")
				.templateMarkdown(markdown)
				.defaultLateFeeType(LateFeeType.FLAT_FEE)
				.defaultLateFeeAmount(new BigDecimal("50.00"))
				.defaultNoticePeriodDays(60)
				.templateParameters(Map.of(
						"parking_spaces", "1",
						"pet_policy", "No pets allowed",
						"utilities_included", "Water and trash",
						"maintenance_responsibility", "Landlord responsible for structural repairs; Tenant responsible for minor maintenance"))
				.active(true)
				.build();

		repository.save(residential);
		log.info("[Data Init] Created: {}", residential.getName());
	}

	private void createCommercialLeaseTemplate() {
		String markdown = loadMarkdownFromResources("commercial-lease-template.md");

		LeaseTemplate commercial = LeaseTemplate.builder()
				.name("Standard Commercial Lease Agreement")
				.versionTag("v1.0")
				.templateMarkdown(markdown)
				.defaultLateFeeType(LateFeeType.PERCENTAGE)
				.defaultLateFeeAmount(new BigDecimal("5.00")) // 5%
				.defaultNoticePeriodDays(60)
				.templateParameters(Map.of(
						"permitted_use", "Retail business operations",
						"operating_hours", "9:00 AM - 9:00 PM, Monday through Saturday",
						"common_area_maintenance", "Tenant responsible for proportionate share",
						"signage_rights", "Subject to landlord approval and local zoning"))
				.active(true)
				.build();

		repository.save(commercial);
		log.info("[Data Init] Created: {}", commercial.getName());
	}

	/**
	 * Loads markdown content from classpath resources.
	 *
	 * @param filename The name of the markdown file (e.g., "residential-lease-template.md")
	 * @return The markdown content as a string
	 * @throws IllegalStateException if the file cannot be read
	 */
	private String loadMarkdownFromResources(String filename) {
		try {
			Resource resource = resourceLoader.getResource(TEMPLATE_BASE_PATH + filename);
			return resource.getContentAsString(StandardCharsets.UTF_8);
		} catch (IOException e) {
			String message = String.format("Failed to load lease template markdown from: %s%s", TEMPLATE_BASE_PATH,
					filename);
			log.error("[Data Init] {}", message, e);
			throw new IllegalStateException(message, e);
		}
	}
}
