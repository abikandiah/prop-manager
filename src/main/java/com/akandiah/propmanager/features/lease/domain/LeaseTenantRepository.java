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
}
