package com.akandiah.propmanager.features.permission.domain;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for permission templates (system and org-scoped).
 */
public interface PermissionTemplateRepository extends JpaRepository<PermissionTemplate, UUID> {

	/**
	 * Templates available for an org: system (org_id IS NULL) plus that org's templates, by name.
	 */
	List<PermissionTemplate> findByOrgIsNullOrOrg_IdOrderByNameAsc(UUID orgId);
}
