package com.akandiah.propmanager.features.tenant.domain;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

	Optional<Tenant> findByUser_Id(UUID userId);
}
