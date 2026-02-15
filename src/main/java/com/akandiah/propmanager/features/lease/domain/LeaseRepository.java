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

	long countByUnit_Id(UUID unitId);

	long countByProperty_Id(UUID propertyId);

	/**
	 * Counts leases that reference this template and are in the given status(es).
	 * Used to block template deletion when any DRAFT lease references it.
	 */
	long countByLeaseTemplate_IdAndStatusIn(UUID leaseTemplateId, java.util.Collection<LeaseStatus> statuses);

	/**
	 * Clears the template reference on all leases that used this template.
	 * Leases keep their denormalized snapshot (leaseTemplateName,
	 * leaseTemplateVersionTag,
	 * executedContentMarkdown) so they remain valid after the template is deleted.
	 */
	@Modifying
	@Query("UPDATE Lease l SET l.leaseTemplate = null WHERE l.leaseTemplate.id = :templateId")
	int clearTemplateReference(@Param("templateId") UUID templateId);
}
