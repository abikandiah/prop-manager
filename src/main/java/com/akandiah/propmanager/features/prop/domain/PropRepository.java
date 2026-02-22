package com.akandiah.propmanager.features.prop.domain;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PropRepository extends JpaRepository<Prop, UUID> {

	long countByOrganization_Id(UUID organizationId);

	boolean existsByIdAndOrganization_Id(UUID id, UUID organizationId);
}
