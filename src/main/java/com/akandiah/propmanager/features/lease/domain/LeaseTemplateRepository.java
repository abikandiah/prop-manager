package com.akandiah.propmanager.features.lease.domain;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaseTemplateRepository extends JpaRepository<LeaseTemplate, UUID> {

	List<LeaseTemplate> findByOrg_Id(UUID orgId);

	List<LeaseTemplate> findByOrg_IdAndActiveTrueOrderByNameAsc(UUID orgId);

	List<LeaseTemplate> findByOrg_IdAndNameContainingIgnoreCaseOrderByNameAsc(UUID orgId, String query);

	long countByOrg_Id(UUID orgId);
}
