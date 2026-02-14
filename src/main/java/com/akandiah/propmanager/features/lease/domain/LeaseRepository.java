package com.akandiah.propmanager.features.lease.domain;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaseRepository extends JpaRepository<Lease, UUID> {

	List<Lease> findByUnit_IdOrderByStartDateDesc(UUID unitId);

	List<Lease> findByProperty_IdOrderByStartDateDesc(UUID propertyId);

	long countByUnit_Id(UUID unitId);

	long countByProperty_Id(UUID propertyId);
}
