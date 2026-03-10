package com.akandiah.propmanager.features.organization.domain;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.akandiah.propmanager.features.lease.domain.LeaseStatus;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

	boolean existsByNameIgnoreCase(String name);

	boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);


	/**
	 * Finds all organizations that the given user belongs to through any relationship.
	 * 1. As a Member (via the Membership table)
	 * 2. As a Property Owner (via the Prop.ownerId field)
	 * 3. As a Tenant on a lease in {@link LeaseStatus#ACTIVE_STATUSES} (invite accepted)
	 */
	@Query("""
			SELECT DISTINCT o FROM Organization o
			WHERE o.id IN (SELECT m.organization.id FROM Membership m WHERE m.user.id = :userId)
			   OR o.id IN (SELECT p.organization.id FROM Prop p WHERE p.ownerId = :userId)
			   OR o.id IN (SELECT p.organization.id FROM LeaseTenant lt
			               JOIN lt.lease l JOIN l.unit u JOIN u.prop p
			               WHERE lt.tenant.user.id = :userId
			               AND lt.tenant IS NOT NULL
			               AND l.status IN :activeStatuses)
			""")
	List<Organization> findAllByUserId(@Param("userId") UUID userId,
			@Param("activeStatuses") Set<LeaseStatus> activeStatuses);

	default List<Organization> findAllByUserId(UUID userId) {
		return findAllByUserId(userId, LeaseStatus.ACTIVE_STATUSES);
	}
}
