package com.akandiah.propmanager.features.lease.domain;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeaseRepository extends JpaRepository<Lease, UUID> {

	List<Lease> findByUnit_IdOrderByStartDateDesc(UUID unitId);

	List<Lease> findByProperty_IdOrderByStartDateDesc(UUID propertyId);

	/** Nulls out the template FK on all leases that reference the given template. */
	@Modifying
	@Query("UPDATE Lease l SET l.leaseTemplate = null WHERE l.leaseTemplate.id = :templateId")
	void clearTemplateReferences(@Param("templateId") UUID templateId);
}
