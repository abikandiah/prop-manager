package com.akandiah.propmanager.features.membership.domain;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for membership templates (system-wide and org-scoped).
 */
public interface MembershipTemplateRepository extends JpaRepository<MembershipTemplate, UUID> {

	/**
	 * Templates available for an org: system templates (org_id IS NULL) plus that org's own templates,
	 * sorted by name.
	 */
	List<MembershipTemplate> findByOrgIsNullOrOrg_IdOrderByNameAsc(UUID orgId);
}
