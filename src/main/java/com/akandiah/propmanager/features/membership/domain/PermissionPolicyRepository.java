package com.akandiah.propmanager.features.membership.domain;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for permission policies (system-wide and org-scoped).
 */
public interface PermissionPolicyRepository extends JpaRepository<PermissionPolicy, UUID> {

	/**
	 * Policies available for an org: system policies (org_id IS NULL) plus that org's own policies,
	 * sorted by name.
	 */
	List<PermissionPolicy> findByOrgIsNullOrOrg_IdOrderByNameAsc(UUID orgId);
}
