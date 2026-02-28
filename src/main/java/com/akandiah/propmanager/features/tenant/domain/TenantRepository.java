package com.akandiah.propmanager.features.tenant.domain;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

	Optional<Tenant> findByUser_Id(UUID userId);

	/**
	 * Returns tenants reachable via leases that fall within the given scoped access
	 * filter. JOIN FETCH on user prevents N+1 when mapping to {@code TenantResponse}.
	 * DISTINCT is required because a tenant may appear on multiple leases.
	 *
	 * @param orgIds     organization IDs the caller has access to
	 * @param propIds    property IDs the caller has access to
	 * @param unitIds    unit IDs the caller has access to
	 * @param activeOnly when true, only tenants on ACTIVE or REVIEW leases are returned
	 */
	@Query("""
			SELECT DISTINCT t FROM Tenant t
			JOIN FETCH t.user u
			JOIN LeaseTenant lt ON lt.tenant = t
			JOIN lt.lease l
			WHERE (l.unit.prop.organization.id IN :orgIds
			   OR l.property.id IN :propIds
			   OR l.unit.id IN :unitIds)
			AND (:activeOnly = false OR l.status IN (
			    com.akandiah.propmanager.features.lease.domain.LeaseStatus.ACTIVE,
			    com.akandiah.propmanager.features.lease.domain.LeaseStatus.REVIEW
			))
			""")
	List<Tenant> findByAccessFilter(
			@Param("orgIds") Set<UUID> orgIds,
			@Param("propIds") Set<UUID> propIds,
			@Param("unitIds") Set<UUID> unitIds,
			@Param("activeOnly") boolean activeOnly);
}
