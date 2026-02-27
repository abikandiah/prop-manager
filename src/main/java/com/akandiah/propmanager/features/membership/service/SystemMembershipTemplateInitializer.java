package com.akandiah.propmanager.features.membership.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.permission.ResourceType;
import com.akandiah.propmanager.features.membership.domain.MembershipTemplate;
import com.akandiah.propmanager.features.membership.domain.MembershipTemplateItem;
import com.akandiah.propmanager.features.membership.domain.MembershipTemplateRepository;

/**
 * Seeds the four standard system-wide membership templates on startup.
 * Idempotent — skips any template whose hardcoded UUID already exists in the DB.
 * An admin can delete a seeded template and restart the service to re-seed it.
 */
@Component
public class SystemMembershipTemplateInitializer implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(SystemMembershipTemplateInitializer.class);

	// Stable hardcoded UUIDs — never change these once deployed
	static final UUID PROPERTY_MANAGER_ID = UUID.fromString("00000000-0000-7000-8000-000000000001");
	static final UUID ORG_ADMIN_ID = UUID.fromString("00000000-0000-7000-8000-000000000002");
	static final UUID MAINTENANCE_ID = UUID.fromString("00000000-0000-7000-8000-000000000003");
	static final UUID VIEWER_ID = UUID.fromString("00000000-0000-7000-8000-000000000004");

	private record SystemTemplateDefinition(UUID id, String name, List<MembershipTemplateItem> items) {}

	private static final List<SystemTemplateDefinition> SYSTEM_TEMPLATES = List.of(
		new SystemTemplateDefinition(
			PROPERTY_MANAGER_ID,
			"Property Manager",
			List.of(new MembershipTemplateItem(
				ResourceType.ORG,
				Map.of("o", "r", "p", "rcud", "l", "rcud", "m", "rcud", "f", "r", "t", "rcud")))
		),
		new SystemTemplateDefinition(
			ORG_ADMIN_ID,
			"Org Admin",
			List.of(new MembershipTemplateItem(
				ResourceType.ORG,
				Map.of("o", "ru", "p", "rcud", "l", "rcud", "m", "rcud", "f", "rcud", "t", "rcud")))
		),
		new SystemTemplateDefinition(
			MAINTENANCE_ID,
			"Maintenance",
			List.of(
				new MembershipTemplateItem(ResourceType.PROPERTY, Map.of("m", "ru")),
				new MembershipTemplateItem(ResourceType.UNIT, Map.of("m", "ru")))
		),
		new SystemTemplateDefinition(
			VIEWER_ID,
			"Viewer",
			List.of(new MembershipTemplateItem(
				ResourceType.ORG,
				Map.of("o", "r", "p", "r", "l", "r", "m", "r", "f", "r", "t", "r")))
		)
	);

	private final MembershipTemplateRepository templateRepository;

	public SystemMembershipTemplateInitializer(MembershipTemplateRepository templateRepository) {
		this.templateRepository = templateRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		for (var def : SYSTEM_TEMPLATES) {
			if (templateRepository.existsById(def.id())) {
				log.debug("[SystemTemplates] '{}' already exists — skipping", def.name());
				continue;
			}
			var template = MembershipTemplate.builder()
					.id(def.id())
					.name(def.name())
					.items(def.items())
					.build();
			templateRepository.save(template);
			log.info("[SystemTemplates] Seeded system membership template '{}'", def.name());
		}
	}
}
