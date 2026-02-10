package com.akandiah.propmanager.features.asset.domain;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetRepository extends JpaRepository<Asset, UUID> {

	List<Asset> findByProp_Id(UUID propId);

	Page<Asset> findByProp_Id(UUID propId, Pageable pageable);

	List<Asset> findByUnit_Id(UUID unitId);

	Page<Asset> findByUnit_Id(UUID unitId, Pageable pageable);

	long countByProp_Id(UUID propId);

	long countByUnit_Id(UUID unitId);
}
