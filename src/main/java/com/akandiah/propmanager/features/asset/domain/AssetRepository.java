package com.akandiah.propmanager.features.asset.domain;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetRepository extends JpaRepository<Asset, UUID> {

	List<Asset> findByProp_Id(UUID propId);

	List<Asset> findByUnit_Id(UUID unitId);

	long countByProp_Id(UUID propId);

	long countByUnit_Id(UUID unitId);
}
