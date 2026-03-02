package com.akandiah.propmanager.features.membership.service;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.features.membership.domain.PermissionPolicy;
import com.akandiah.propmanager.features.membership.domain.PermissionPolicyRepository;

/**
 * Seeds the four standard system-wide permission policies on startup.
 * Idempotent — skips any policy whose hardcoded UUID already exists in the DB.
 * An admin can delete a seeded policy and restart the service to re-seed it.
 */
@Component
@Order(1)
public class SystemPermissionPolicyInitializer implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(SystemPermissionPolicyInitializer.class);

	// Stable hardcoded UUIDs — never change these once deployed
	public static final UUID PROPERTY_MANAGER_ID = UUID.fromString("00000000-0000-7000-8000-000000000001");
	public static final UUID ORG_ADMIN_ID = UUID.fromString("00000000-0000-7000-8000-000000000002");
	public static final UUID MAINTENANCE_ID = UUID.fromString("00000000-0000-7000-8000-000000000003");
	public static final UUID VIEWER_ID = UUID.fromString("00000000-0000-7000-8000-000000000004");
	/** Full CRUD across all domains — assigned to the user who creates an organization. */
	public static final UUID ORG_OWNER_ID = UUID.fromString("00000000-0000-7000-8000-000000000005");

	private record SystemPolicyDefinition(UUID id, String name, Map<String, String> permissions) {}

	private static final java.util.List<SystemPolicyDefinition> SYSTEM_POLICIES = java.util.List.of(
		new SystemPolicyDefinition(
			PROPERTY_MANAGER_ID,
			"Property Manager",
			Map.of("o", "r", "p", "rcud", "l", "rcud", "m", "rcud", "f", "r", "t", "rcud")
		),
		new SystemPolicyDefinition(
			ORG_ADMIN_ID,
			"Org Admin",
			Map.of("o", "ru", "p", "rcud", "l", "rcud", "m", "rcud", "f", "rcud", "t", "rcud")
		),
		new SystemPolicyDefinition(
			MAINTENANCE_ID,
			"Maintenance",
			Map.of("m", "ru")
		),
		new SystemPolicyDefinition(
			VIEWER_ID,
			"Viewer",
			Map.of("o", "r", "p", "r", "l", "r", "m", "r", "f", "r", "t", "r")
		),
		new SystemPolicyDefinition(
			ORG_OWNER_ID,
			"Org Owner",
			Map.of("o", "rcud", "p", "rcud", "l", "rcud", "m", "rcud", "f", "rcud", "t", "rcud")
		)
	);

	private final PermissionPolicyRepository policyRepository;

	public SystemPermissionPolicyInitializer(PermissionPolicyRepository policyRepository) {
		this.policyRepository = policyRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		for (var def : SYSTEM_POLICIES) {
			if (policyRepository.existsById(def.id())) {
				log.debug("[SystemPolicies] '{}' already exists — skipping", def.name());
				continue;
			}
			var policy = PermissionPolicy.builder()
					.id(def.id())
					.name(def.name())
					.permissions(new java.util.HashMap<>(def.permissions()))
					.build();
			policyRepository.save(policy);
			log.info("[SystemPolicies] Seeded system permission policy '{}'", def.name());
		}
	}
}
