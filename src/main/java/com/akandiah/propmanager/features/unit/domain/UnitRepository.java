package com.akandiah.propmanager.features.unit.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UnitRepository extends JpaRepository<Unit, UUID> {

	List<Unit> findByProp_IdOrderByUnitNumberAsc(UUID propId);

	long countByProp_Id(UUID propId);

	boolean existsByIdAndProp_Organization_Id(UUID id, UUID organizationId);

	/** Fetches a unit with its prop and org in a single join — avoids the N+1 in hierarchy resolution. */
	@Query("SELECT u FROM Unit u JOIN FETCH u.prop p JOIN FETCH p.organization WHERE u.id = :id")
	Optional<Unit> findByIdWithPropAndOrg(@Param("id") UUID id);

	@Query("""
			SELECT u FROM Unit u
			WHERE u.prop.organization.id IN :orgIds
			   OR u.prop.id IN :propIds
			   OR u.id IN :unitIds
			""")
	List<Unit> findByAccessFilter(
			@Param("orgIds") Collection<UUID> orgIds,
			@Param("propIds") Collection<UUID> propIds,
			@Param("unitIds") Collection<UUID> unitIds);
}
