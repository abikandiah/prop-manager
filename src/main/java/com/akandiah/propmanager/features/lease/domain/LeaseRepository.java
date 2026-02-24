package com.akandiah.propmanager.features.lease.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeaseRepository extends JpaRepository<Lease, UUID> {

	List<Lease> findByUnit_IdOrderByStartDateDesc(UUID unitId);

	List<Lease> findByProperty_IdOrderByStartDateDesc(UUID propertyId);

	long countByUnit_Id(UUID unitId);

	long countByProperty_Id(UUID propertyId);

	/**
	 * Counts leases that reference this template and are in the given status(es).
	 * Used to block template deletion when any DRAFT lease references it.
	 */
	long countByLeaseTemplate_IdAndStatusIn(UUID leaseTemplateId, java.util.Collection<LeaseStatus> statuses);

	/**
	 * Checks whether a unit already has an active lease, excluding the given lease ID.
	 * Used before activation to prevent concurrent active leases on the same unit.
	 */
	boolean existsByUnit_IdAndStatusAndIdNot(UUID unitId, LeaseStatus status, UUID excludedLeaseId);

	@Modifying
	@Query("UPDATE Lease l SET l.leaseTemplate = null WHERE l.leaseTemplate.id = :templateId")
	int clearTemplateReference(@Param("templateId") UUID templateId);

	/**
	 * Find active leases expiring on a specific date.
	 * Used by the expiry scheduler to send advance notice.
	 */
	List<Lease> findByStatusAndEndDate(LeaseStatus status, LocalDate endDate);

	/**
	 * Fetch lease with unit and property eagerly loaded.
	 * Used by the NotificationDispatcher on an async thread where no Hibernate session is active.
	 */
	@Query("SELECT l FROM Lease l JOIN FETCH l.unit JOIN FETCH l.property WHERE l.id = :id")
	Optional<Lease> findByIdWithUnitAndProperty(@Param("id") UUID id);

	/**
	 * Fetch lease with unit, property and property address eagerly loaded.
	 * Used by the invite preview endpoint to resolve all contextual data in one query.
	 */
	@Query("SELECT l FROM Lease l JOIN FETCH l.unit JOIN FETCH l.property p JOIN FETCH p.address WHERE l.id = :id")
	Optional<Lease> findByIdWithUnitPropertyAndAddress(@Param("id") UUID id);

	/**
	 * Fetch lease with unit, unit's prop, and prop's organization eagerly loaded.
	 * Used for authorization checks that require the full resource hierarchy.
	 */
	@Query("SELECT l FROM Lease l JOIN FETCH l.unit u JOIN FETCH u.prop p JOIN FETCH p.organization WHERE l.id = :id")
	Optional<Lease> findByIdWithUnitPropAndOrg(@Param("id") UUID id);

	@Query("SELECT l.unit.id FROM Lease l WHERE l.id = :id")
	Optional<UUID> findUnitIdById(@Param("id") UUID id);

	@Query("""
			SELECT l FROM Lease l
			WHERE l.unit.prop.organization.id IN :orgIds
			   OR l.property.id IN :propIds
			   OR l.unit.id IN :unitIds
			""")
	List<Lease> findByAccessFilter(
			@Param("orgIds") Collection<UUID> orgIds,
			@Param("propIds") Collection<UUID> propIds,
			@Param("unitIds") Collection<UUID> unitIds);
}
