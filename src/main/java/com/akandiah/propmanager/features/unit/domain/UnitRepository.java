package com.akandiah.propmanager.features.unit.domain;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UnitRepository extends JpaRepository<Unit, UUID> {

	List<Unit> findByProp_IdOrderByUnitNumberAsc(UUID propId);
}
