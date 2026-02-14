package com.akandiah.propmanager.features.asset.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.common.util.OptimisticLockingUtil;
import com.akandiah.propmanager.features.asset.api.dto.AssetResponse;
import com.akandiah.propmanager.features.asset.api.dto.CreateAssetRequest;
import com.akandiah.propmanager.features.asset.api.dto.UpdateAssetRequest;
import com.akandiah.propmanager.features.asset.domain.Asset;
import com.akandiah.propmanager.features.asset.domain.AssetRepository;
import com.akandiah.propmanager.features.prop.domain.Prop;
import com.akandiah.propmanager.features.prop.domain.PropRepository;
import com.akandiah.propmanager.features.unit.domain.Unit;
import com.akandiah.propmanager.features.unit.domain.UnitRepository;

@Service
public class AssetService {

	private final AssetRepository assetRepository;
	private final PropRepository propRepository;
	private final UnitRepository unitRepository;

	public AssetService(AssetRepository assetRepository, PropRepository propRepository,
			UnitRepository unitRepository) {
		this.assetRepository = assetRepository;
		this.propRepository = propRepository;
		this.unitRepository = unitRepository;
	}

	@Transactional(readOnly = true)
	public List<AssetResponse> findAll() {
		return assetRepository.findAll().stream()
				.map(AssetResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<AssetResponse> findByPropId(UUID propId) {
		return assetRepository.findByProp_Id(propId).stream()
				.map(AssetResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<AssetResponse> findByUnitId(UUID unitId) {
		return assetRepository.findByUnit_Id(unitId).stream()
				.map(AssetResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public AssetResponse findById(UUID id) {
		Asset asset = assetRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Asset", id));
		return AssetResponse.from(asset);
	}

	@Transactional
	public AssetResponse create(CreateAssetRequest request) {
		validateExactlyOneParent(request.propertyId(), request.unitId());
		Prop prop = null;
		Unit unit = null;
		if (request.propertyId() != null) {
			prop = propRepository.findById(request.propertyId())
					.orElseThrow(() -> new ResourceNotFoundException("Prop", request.propertyId()));
		} else {
			unit = unitRepository.findById(request.unitId())
					.orElseThrow(() -> new ResourceNotFoundException("Unit", request.unitId()));
		}
		Asset asset = Asset.builder()
				.prop(prop)
				.unit(unit)
				.category(request.category())
				.makeModel(request.makeModel())
				.serialNumber(request.serialNumber())
				.installDate(request.installDate())
				.warrantyExpiry(request.warrantyExpiry())
				.lastServiceDate(request.lastServiceDate())
				.build();
		asset = assetRepository.save(asset);
		return AssetResponse.from(asset);
	}

	@Transactional
	public AssetResponse update(UUID id, UpdateAssetRequest request) {
		Asset asset = assetRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Asset", id));
		OptimisticLockingUtil.requireVersionMatch("Asset", id, asset.getVersion(), request.version());
		if (request.propertyId() != null || request.unitId() != null) {
			boolean hasProp = request.propertyId() != null;
			boolean hasUnit = request.unitId() != null;
			if (hasProp == hasUnit) {
				throw new IllegalArgumentException(
						"Exactly one of propertyId or unitId must be set");
			}
			if (hasProp) {
				Prop prop = propRepository.findById(request.propertyId())
						.orElseThrow(() -> new ResourceNotFoundException("Prop", request.propertyId()));
				asset.setProp(prop);
				asset.setUnit(null);
			} else {
				Unit unit = unitRepository.findById(request.unitId())
						.orElseThrow(() -> new ResourceNotFoundException("Unit", request.unitId()));
				asset.setUnit(unit);
				asset.setProp(null);
			}
		}
		if (request.category() != null) {
			asset.setCategory(request.category());
		}
		if (request.makeModel() != null) {
			asset.setMakeModel(request.makeModel());
		}
		if (request.serialNumber() != null) {
			asset.setSerialNumber(request.serialNumber());
		}
		if (request.installDate() != null) {
			asset.setInstallDate(request.installDate());
		}
		if (request.warrantyExpiry() != null) {
			asset.setWarrantyExpiry(request.warrantyExpiry());
		}
		if (request.lastServiceDate() != null) {
			asset.setLastServiceDate(request.lastServiceDate());
		}
		asset = assetRepository.save(asset);
		return AssetResponse.from(asset);
	}

	@Transactional
	public void deleteById(UUID id) {
		if (!assetRepository.existsById(id)) {
			throw new ResourceNotFoundException("Asset", id);
		}
		assetRepository.deleteById(id);
	}

	private void validateExactlyOneParent(UUID propertyId, UUID unitId) {
		boolean hasProp = propertyId != null;
		boolean hasUnit = unitId != null;
		if (!hasProp && !hasUnit) {
			throw new IllegalArgumentException(
					"Exactly one of propertyId or unitId is required");
		}
		if (hasProp && hasUnit) {
			throw new IllegalArgumentException(
					"Only one of propertyId or unitId may be set, not both");
		}
	}
}
