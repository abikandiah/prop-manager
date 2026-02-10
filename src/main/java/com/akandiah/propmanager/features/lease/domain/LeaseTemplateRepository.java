package com.akandiah.propmanager.features.lease.domain;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaseTemplateRepository extends JpaRepository<LeaseTemplate, UUID> {

	List<LeaseTemplate> findByActiveTrueOrderByNameAsc();

	Page<LeaseTemplate> findByActiveTrueOrderByNameAsc(Pageable pageable);

	List<LeaseTemplate> findByNameContainingIgnoreCaseOrderByNameAsc(String name);

	Page<LeaseTemplate> findByNameContainingIgnoreCaseOrderByNameAsc(String name, Pageable pageable);
}
