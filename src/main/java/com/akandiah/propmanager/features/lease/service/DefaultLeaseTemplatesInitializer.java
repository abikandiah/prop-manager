package com.akandiah.propmanager.features.lease.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
import com.akandiah.propmanager.features.organization.domain.Organization;
import com.akandiah.propmanager.features.organization.domain.OrganizationRepository;

/**
 * Initializes default lease templates on first application startup, per organization.
 * Idempotent — only seeds templates for orgs that have none yet.
 */
@Component
public class DefaultLeaseTemplatesInitializer implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DefaultLeaseTemplatesInitializer.class);
	private static final String TEMPLATE_BASE_PATH = "classpath:templates/lease/";

	private final LeaseTemplateRepository repository;
	private final OrganizationRepository organizationRepository;
	private final ResourceLoader resourceLoader;

	public DefaultLeaseTemplatesInitializer(LeaseTemplateRepository repository,
			OrganizationRepository organizationRepository,
			ResourceLoader resourceLoader) {
		this.repository = repository;
		this.organizationRepository = organizationRepository;
		this.resourceLoader = resourceLoader;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		List<Organization> orgs = organizationRepository.findAll();
		if (orgs.isEmpty()) {
			log.info("[Data Init] No organizations found, skipping lease template seeding");
			return;
		}

		for (Organization org : orgs) {
			long count = repository.countByOrg_Id(org.getId());
			if (count > 0) {
				log.info("[Data Init] Org '{}' already has {} lease template(s), skipping", org.getId(), count);
				continue;
			}

			log.info("[Data Init] Seeding default lease templates for org '{}'", org.getId());
			createResidentialLeaseTemplate(org);
			createCommercialLeaseTemplate(org);
			log.info("[Data Init] Default lease templates created for org '{}'", org.getId());
		}
	}

	private void createResidentialLeaseTemplate(Organization org) {
		String markdown = loadMarkdownFromResources("residential-lease-template.md");

		LeaseTemplate residential = LeaseTemplate.builder()
				.org(org)
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

	private void createCommercialLeaseTemplate(Organization org) {
		String markdown = loadMarkdownFromResources("commercial-lease-template.md");

		LeaseTemplate commercial = LeaseTemplate.builder()
				.org(org)
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
