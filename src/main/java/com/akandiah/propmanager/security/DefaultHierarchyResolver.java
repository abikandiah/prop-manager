package com.akandiah.propmanager.security;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.permission.ResourceType;
import com.akandiah.propmanager.common.permission.ScopeLevel;
import com.akandiah.propmanager.common.permission.HierarchyResolver;
import com.akandiah.propmanager.features.prop.domain.Prop;
import com.akandiah.propmanager.features.prop.domain.PropRepository;
import com.akandiah.propmanager.features.unit.domain.UnitRepository;

import lombok.RequiredArgsConstructor;

/**
 * Resolves org → property → unit hierarchy using Unit and Prop repositories.
 */
@Component
@RequiredArgsConstructor
public class DefaultHierarchyResolver implements HierarchyResolver {

	private final UnitRepository unitRepository;
	private final PropRepository propRepository;

	@Override
	@Transactional(readOnly = true)
	public List<ScopeLevel> resolve(ResourceType resourceType, UUID resourceId, UUID orgId) {
		return switch (resourceType) {
			case ORG -> List.of(new ScopeLevel("ORG", orgId));
			case PROPERTY -> resolveProperty(resourceId, orgId);
			case UNIT -> resolveUnit(resourceId, orgId);
		};
	}

	private List<ScopeLevel> resolveProperty(UUID propId, UUID orgId) {
		if (!propRepository.existsByIdAndOrganization_Id(propId, orgId)) {
			return List.of();
		}
		List<ScopeLevel> chain = new ArrayList<>();
		chain.add(new ScopeLevel("PROPERTY", propId));
		chain.add(new ScopeLevel("ORG", orgId));
		return chain;
	}

	private List<ScopeLevel> resolveUnit(UUID unitId, UUID orgId) {
		return unitRepository.findById(unitId)
				.filter(unit -> unit.getProp() != null && unit.getProp().getOrganization() != null)
				.filter(unit -> unit.getProp().getOrganization().getId().equals(orgId))
				.map(unit -> {
					Prop prop = unit.getProp();
					UUID propId = prop.getId();
					UUID resolvedOrgId = prop.getOrganization().getId();
					List<ScopeLevel> chain = new ArrayList<>();
					chain.add(new ScopeLevel("UNIT", unitId));
					chain.add(new ScopeLevel("PROPERTY", propId));
					chain.add(new ScopeLevel("ORG", resolvedOrgId));
					return chain;
				})
				.orElse(List.of());
	}
}
