package com.akandiah.propmanager.features.asset.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssetRepository extends JpaRepository<Asset, UUID> {

	List<Asset> findByProp_Id(UUID propId);

	List<Asset> findByUnit_Id(UUID unitId);

	long countByProp_Id(UUID propId);

	long countByUnit_Id(UUID unitId);

	boolean existsByIdAndProp_Organization_Id(UUID id, UUID orgId);

	boolean existsByIdAndUnit_Prop_Organization_Id(UUID id, UUID orgId);

	/**
	 * Loads an asset with its full parent chain in one query to avoid N+1 in the
	 * hierarchy resolver. Fetches: prop → org (for property-scoped assets) and
	 * unit → prop → org (for unit-scoped assets).
	 */
	@Query("""
			SELECT a FROM Asset a
			LEFT JOIN FETCH a.prop p
			LEFT JOIN FETCH p.organization
			LEFT JOIN FETCH a.unit u
			LEFT JOIN FETCH u.prop up
			LEFT JOIN FETCH up.organization
			WHERE a.id = :id
			""")
	Optional<Asset> findByIdWithParents(@Param("id") UUID id);
}
