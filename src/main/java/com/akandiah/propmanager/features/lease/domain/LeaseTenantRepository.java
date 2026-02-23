package com.akandiah.propmanager.features.lease.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeaseTenantRepository extends JpaRepository<LeaseTenant, UUID> {

	List<LeaseTenant> findByLease_Id(UUID leaseId);

	List<LeaseTenant> findByTenant_Id(UUID tenantId);

	long countByLease_Id(UUID leaseId);

	Optional<LeaseTenant> findByInvite_Id(UUID inviteId);

	/** Returns true if the user is a tenant on the given lease. */
	boolean existsByLease_IdAndTenant_User_Id(UUID leaseId, UUID userId);

	/** Returns true if the user is a tenant on any lease belonging to the given unit. */
	boolean existsByLease_Unit_IdAndTenant_User_Id(UUID unitId, UUID userId);

	/**
	 * Fetch lease-tenant slots with tenant and user eagerly loaded.
	 * Only returns rows where tenant is non-null (invite accepted).
	 * Used by the NotificationDispatcher on an async thread where no Hibernate session is active.
	 */
	@Query("""
			SELECT lt FROM LeaseTenant lt
			JOIN FETCH lt.tenant t
			JOIN FETCH t.user
			WHERE lt.lease.id = :leaseId
			AND lt.tenant IS NOT NULL
			""")
	List<LeaseTenant> findByLease_IdWithTenantUser(@Param("leaseId") UUID leaseId);

	/**
	 * Find active lease-tenant rows for a given user, eagerly loading the full chain
	 * needed for permission hydration: tenant → user, lease → unit → prop → organization.
	 * Only returns rows where:
	 * - tenant is non-null (invite accepted)
	 * - lease status is ACTIVE or REVIEW (terminated/expired leases grant no access)
	 * - prop has an organization (org-less props are excluded)
	 */
	@Query("""
			SELECT lt FROM LeaseTenant lt
			JOIN FETCH lt.tenant t
			JOIN FETCH t.user u
			JOIN FETCH lt.lease l
			JOIN FETCH l.unit unit
			JOIN FETCH unit.prop p
			JOIN FETCH p.organization org
			WHERE u.id = :userId
			AND lt.tenant IS NOT NULL
			AND l.status IN (com.akandiah.propmanager.features.lease.domain.LeaseStatus.ACTIVE,
			                 com.akandiah.propmanager.features.lease.domain.LeaseStatus.REVIEW)
			AND p.organization IS NOT NULL
			""")
	List<LeaseTenant> findActiveByUserIdWithLeaseUnitPropOrg(@Param("userId") UUID userId);
}
