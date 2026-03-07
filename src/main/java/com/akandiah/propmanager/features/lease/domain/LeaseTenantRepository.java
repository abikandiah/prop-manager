package com.akandiah.propmanager.features.lease.domain;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeaseTenantRepository extends JpaRepository<LeaseTenant, UUID> {

	List<LeaseTenant> findByLease_Id(UUID leaseId);

	List<LeaseTenant> findByTenant_Id(UUID tenantId);

	long countByLease_Id(UUID leaseId);

	/** Counts tenant slots on a lease that do NOT have the given status. Used to track signature progress. */
	long countByLease_IdAndStatusNot(UUID leaseId, LeaseTenantStatus status);

	/**
	 * Counts tenant slots that still need to sign for the given lease version —
	 * either not yet SIGNED, or SIGNED on a stale version (re-sign required after revert).
	 */
	@Query("""
			SELECT COUNT(lt) FROM LeaseTenant lt
			WHERE lt.lease.id = :leaseId
			AND (lt.status != com.akandiah.propmanager.features.lease.domain.LeaseTenantStatus.SIGNED
			     OR lt.acceptedLeaseVersion != :leaseVersion)
			""")
	long countNotSignedForVersion(@Param("leaseId") UUID leaseId, @Param("leaseVersion") Integer leaseVersion);

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
	 * - lease status is in {@link LeaseStatus#ACTIVE_STATUSES}
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
			AND l.status IN :activeStatuses
			AND p.organization IS NOT NULL
			""")
	List<LeaseTenant> findActiveByUserIdWithLeaseUnitPropOrg(@Param("userId") UUID userId,
			@Param("activeStatuses") Set<LeaseStatus> activeStatuses);

	default List<LeaseTenant> findActiveByUserIdWithLeaseUnitPropOrg(UUID userId) {
		return findActiveByUserIdWithLeaseUnitPropOrg(userId, LeaseStatus.ACTIVE_STATUSES);
	}

	/**
	 * Returns the unit IDs for all leases in {@link LeaseStatus#ACTIVE_STATUSES} that the given
	 * tenant is attached to. Used by {@code PermissionGuard.hasTenantAccess} to resolve a tenant
	 * to their unit(s) for hierarchy-aware access checks.
	 */
	@Query("""
			SELECT l.unit.id FROM LeaseTenant lt
			JOIN lt.lease l
			WHERE lt.tenant.id = :tenantId
			AND l.status IN :activeStatuses
			""")
	List<UUID> findUnitIdsByTenantId(@Param("tenantId") UUID tenantId,
			@Param("activeStatuses") Set<LeaseStatus> activeStatuses);

	default List<UUID> findUnitIdsByTenantId(UUID tenantId) {
		return findUnitIdsByTenantId(tenantId, LeaseStatus.ACTIVE_STATUSES);
	}

	/**
	 * Finds active lease-tenant slots for a tenant in a specific org.
	 * JOIN FETCH loads lease → property → organization and lease → unit to avoid N+1 in TenantPortalService.
	 */
	@Query("""
			SELECT lt FROM LeaseTenant lt
			JOIN FETCH lt.lease l
			JOIN FETCH l.property prop
			JOIN FETCH prop.organization org
			JOIN FETCH l.unit unit
			WHERE lt.tenant.id = :tenantId
			AND l.status IN :statuses
			AND prop.organization.id = :orgId
			""")
	List<LeaseTenant> findByTenantIdAndStatusInAndOrgId(
			@Param("tenantId") UUID tenantId,
			@Param("statuses") Set<LeaseStatus> statuses,
			@Param("orgId") UUID orgId);

	/**
	 * Fetches all tenant slots for a lease with tenant+user and invite eagerly loaded.
	 * Used to build co-tenant summaries in TenantPortalService.
	 */
	@Query("""
			SELECT lt FROM LeaseTenant lt
			LEFT JOIN FETCH lt.tenant t
			LEFT JOIN FETCH t.user
			JOIN FETCH lt.invite
			WHERE lt.lease.id = :leaseId
			""")
	List<LeaseTenant> findByLeaseIdWithTenantAndInvite(@Param("leaseId") UUID leaseId);
}
