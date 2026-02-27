package com.akandiah.propmanager.security;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.permission.ResourceType;
import com.akandiah.propmanager.common.permission.ScopeLevel;
import com.akandiah.propmanager.common.permission.HierarchyResolver;
import com.akandiah.propmanager.features.asset.domain.AssetRepository;
import com.akandiah.propmanager.features.prop.domain.Prop;
import com.akandiah.propmanager.features.prop.domain.PropRepository;
import com.akandiah.propmanager.features.unit.domain.UnitRepository;

import lombok.RequiredArgsConstructor;

/** Resolves org → property → unit → asset hierarchy. */
@Component
@RequiredArgsConstructor
public class DefaultHierarchyResolver implements HierarchyResolver {

	private final UnitRepository unitRepository;
	private final PropRepository propRepository;
	private final AssetRepository assetRepository;

	@Override
	@Transactional(readOnly = true)
	public List<ScopeLevel> resolve(ResourceType resourceType, UUID resourceId, UUID orgId) {
		return switch (resourceType) {
			case ORG -> List.of(new ScopeLevel(ResourceType.ORG, orgId));
			case PROPERTY -> resolveProperty(resourceId, orgId);
			case UNIT -> resolveUnit(resourceId, orgId);
			case ASSET -> resolveAsset(resourceId, orgId);
		};
	}

	private List<ScopeLevel> resolveProperty(UUID propId, UUID orgId) {
		if (!propRepository.existsByIdAndOrganization_Id(propId, orgId)) {
			return List.of();
		}
		List<ScopeLevel> chain = new ArrayList<>();
		chain.add(new ScopeLevel(ResourceType.PROPERTY, propId));
		chain.add(new ScopeLevel(ResourceType.ORG, orgId));
		return chain;
	}

	private List<ScopeLevel> resolveUnit(UUID unitId, UUID orgId) {
		return unitRepository.findByIdWithPropAndOrg(unitId)
				.filter(unit -> unit.getProp() != null && unit.getProp().getOrganization() != null)
				.filter(unit -> unit.getProp().getOrganization().getId().equals(orgId))
				.map(unit -> {
					Prop prop = unit.getProp();
					UUID propId = prop.getId();
					UUID resolvedOrgId = prop.getOrganization().getId();
					List<ScopeLevel> chain = new ArrayList<>();
					chain.add(new ScopeLevel(ResourceType.UNIT, unitId));
					chain.add(new ScopeLevel(ResourceType.PROPERTY, propId));
					chain.add(new ScopeLevel(ResourceType.ORG, resolvedOrgId));
					return chain;
				})
				.orElse(List.of());
	}

	private List<ScopeLevel> resolveAsset(UUID assetId, UUID orgId) {
		return assetRepository.findByIdWithParents(assetId)
				.map(asset -> {
					List<ScopeLevel> chain = new ArrayList<>();
					chain.add(new ScopeLevel(ResourceType.ASSET, assetId));

					if (asset.getUnit() != null) {
						// Unit-scoped asset: [ASSET -> UNIT -> PROPERTY -> ORG]
						if (asset.getUnit().getProp() == null ||
								asset.getUnit().getProp().getOrganization() == null ||
								!asset.getUnit().getProp().getOrganization().getId().equals(orgId)) {
							return List.<ScopeLevel>of();
						}
						chain.add(new ScopeLevel(ResourceType.UNIT, asset.getUnit().getId()));
						chain.add(new ScopeLevel(ResourceType.PROPERTY, asset.getUnit().getProp().getId()));
					} else if (asset.getProp() != null) {
						// Property-scoped asset: [ASSET -> PROPERTY -> ORG]
						if (asset.getProp().getOrganization() == null ||
								!asset.getProp().getOrganization().getId().equals(orgId)) {
							return List.<ScopeLevel>of();
						}
						chain.add(new ScopeLevel(ResourceType.PROPERTY, asset.getProp().getId()));
					}

					chain.add(new ScopeLevel(ResourceType.ORG, orgId));
					return chain;
				})
				.orElse(List.of());
	}
}
