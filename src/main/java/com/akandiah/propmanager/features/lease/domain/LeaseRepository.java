package com.akandiah.propmanager.features.lease.domain;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaseRepository extends JpaRepository<Lease, UUID> {

	List<Lease> findByUnit_IdOrderByStartDateDesc(UUID unitId);

	Page<Lease> findByUnit_IdOrderByStartDateDesc(UUID unitId, Pageable pageable);

	List<Lease> findByProperty_IdOrderByStartDateDesc(UUID propertyId);

	Page<Lease> findByProperty_IdOrderByStartDateDesc(UUID propertyId, Pageable pageable);

	long countByUnit_Id(UUID unitId);

	long countByProperty_Id(UUID propertyId);
}
