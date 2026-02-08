package com.akandiah.propmanager.features.lease.domain;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaseTenantRepository extends JpaRepository<LeaseTenant, UUID> {

	List<LeaseTenant> findByLease_Id(UUID leaseId);

	List<LeaseTenant> findByTenant_Id(UUID tenantId);

	long countByLease_Id(UUID leaseId);
}
