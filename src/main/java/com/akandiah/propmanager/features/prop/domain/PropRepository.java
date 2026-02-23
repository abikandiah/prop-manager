package com.akandiah.propmanager.features.prop.domain;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PropRepository extends JpaRepository<Prop, UUID> {

	long countByOrganization_Id(UUID organizationId);

	boolean existsByIdAndOrganization_Id(UUID id, UUID organizationId);

	@Query("SELECT p FROM Prop p JOIN FETCH p.organization WHERE p.ownerId = :userId AND p.organization IS NOT NULL")
	List<Prop> findByOwnerIdWithOrganization(@Param("userId") UUID userId);

	@Query("SELECT p FROM Prop p WHERE p.organization.id IN :orgIds OR p.id IN :propIds")
	List<Prop> findByOrganizationIdInOrIdIn(
			@Param("orgIds") java.util.Collection<UUID> orgIds,
			@Param("propIds") java.util.Collection<UUID> propIds);
}
