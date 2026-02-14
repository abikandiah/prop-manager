package com.akandiah.propmanager.features.lease.domain;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaseTemplateRepository extends JpaRepository<LeaseTemplate, UUID> {

	List<LeaseTemplate> findByActiveTrueOrderByNameAsc();

	List<LeaseTemplate> findByNameContainingIgnoreCaseOrderByNameAsc(String name);
}
