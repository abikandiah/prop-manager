package com.akandiah.propmanager.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.akandiah.propmanager.common.permission.ResourceType;
import com.akandiah.propmanager.common.permission.ScopeLevel;
import com.akandiah.propmanager.features.asset.domain.Asset;
import com.akandiah.propmanager.features.asset.domain.AssetRepository;
import com.akandiah.propmanager.features.organization.domain.Organization;
import com.akandiah.propmanager.features.prop.domain.Prop;
import com.akandiah.propmanager.features.prop.domain.PropRepository;
import com.akandiah.propmanager.features.unit.domain.Unit;
import com.akandiah.propmanager.features.unit.domain.UnitRepository;
import com.akandiah.propmanager.features.unit.domain.UnitStatus;

@ExtendWith(MockitoExtension.class)
class DefaultHierarchyResolverTest {

	@Mock
	private UnitRepository unitRepository;
	@Mock
	private PropRepository propRepository;
	@Mock
	private AssetRepository assetRepository;

	private DefaultHierarchyResolver resolver;

	private static final UUID ORG_ID = UUID.randomUUID();
	private static final UUID PROP_ID = UUID.randomUUID();
	private static final UUID UNIT_ID = UUID.randomUUID();
	private static final UUID ASSET_ID = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		resolver = new DefaultHierarchyResolver(unitRepository, propRepository, assetRepository);
	}

	@Test
	void resolve_orgReturnsSingleLevel() {
		List<ScopeLevel> result = resolver.resolve(ResourceType.ORG, ORG_ID, ORG_ID);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).scopeType()).isEqualTo(ResourceType.ORG);
		assertThat(result.get(0).scopeId()).isEqualTo(ORG_ID);
	}

	@Test
	void resolve_propertyReturnsPropertyThenOrgWhenExistsInOrg() {
		when(propRepository.existsByIdAndOrganization_Id(PROP_ID, ORG_ID)).thenReturn(true);

		List<ScopeLevel> result = resolver.resolve(ResourceType.PROPERTY, PROP_ID, ORG_ID);

		assertThat(result).hasSize(2);
		assertThat(result.get(0).scopeType()).isEqualTo(ResourceType.PROPERTY);
		assertThat(result.get(0).scopeId()).isEqualTo(PROP_ID);
		assertThat(result.get(1).scopeType()).isEqualTo(ResourceType.ORG);
		assertThat(result.get(1).scopeId()).isEqualTo(ORG_ID);
	}

	@Test
	void resolve_propertyReturnsEmptyWhenNotInOrg() {
		when(propRepository.existsByIdAndOrganization_Id(PROP_ID, ORG_ID)).thenReturn(false);

		List<ScopeLevel> result = resolver.resolve(ResourceType.PROPERTY, PROP_ID, ORG_ID);

		assertThat(result).isEmpty();
	}

	@Test
	void resolve_unitReturnsUnitThenPropertyThenOrgWhenInOrg() {
		Organization org = Organization.builder().id(ORG_ID).build();
		Prop prop = Prop.builder().id(PROP_ID).organization(org).build();
		Unit unit = Unit.builder()
				.id(UNIT_ID)
				.prop(prop)
				.unitNumber("101")
				.status(UnitStatus.VACANT)
				.build();
		when(unitRepository.findByIdWithPropAndOrg(UNIT_ID)).thenReturn(Optional.of(unit));

		List<ScopeLevel> result = resolver.resolve(ResourceType.UNIT, UNIT_ID, ORG_ID);

		assertThat(result).hasSize(3);
		assertThat(result.get(0).scopeType()).isEqualTo(ResourceType.UNIT);
		assertThat(result.get(0).scopeId()).isEqualTo(UNIT_ID);
		assertThat(result.get(1).scopeType()).isEqualTo(ResourceType.PROPERTY);
		assertThat(result.get(1).scopeId()).isEqualTo(PROP_ID);
		assertThat(result.get(2).scopeType()).isEqualTo(ResourceType.ORG);
		assertThat(result.get(2).scopeId()).isEqualTo(ORG_ID);
	}

	@Test
	void resolve_unitReturnsEmptyWhenUnitNotFound() {
		when(unitRepository.findByIdWithPropAndOrg(UNIT_ID)).thenReturn(Optional.empty());

		List<ScopeLevel> result = resolver.resolve(ResourceType.UNIT, UNIT_ID, ORG_ID);

		assertThat(result).isEmpty();
	}

	@Test
	void resolve_unitReturnsEmptyWhenUnitInDifferentOrg() {
		UUID otherOrgId = UUID.randomUUID();
		Organization org = Organization.builder().id(otherOrgId).build();
		Prop prop = Prop.builder().id(PROP_ID).organization(org).build();
		Unit unit = Unit.builder().id(UNIT_ID).prop(prop).build();
		when(unitRepository.findByIdWithPropAndOrg(UNIT_ID)).thenReturn(Optional.of(unit));

		List<ScopeLevel> result = resolver.resolve(ResourceType.UNIT, UNIT_ID, ORG_ID);

		assertThat(result).isEmpty();
	}

	// ─────────────────── resolveAsset ───────────────────

	@Test
	void resolve_asset_unitScoped_returnsChain() {
		Organization org = Organization.builder().id(ORG_ID).build();
		Prop prop = Prop.builder().id(PROP_ID).organization(org).build();
		Unit unit = Unit.builder().id(UNIT_ID).prop(prop).build();
		Asset asset = Asset.builder().id(ASSET_ID).unit(unit).build();
		when(assetRepository.findByIdWithParents(ASSET_ID)).thenReturn(Optional.of(asset));

		List<ScopeLevel> result = resolver.resolve(ResourceType.ASSET, ASSET_ID, ORG_ID);

		assertThat(result).hasSize(4);
		assertThat(result.get(0).scopeType()).isEqualTo(ResourceType.ASSET);
		assertThat(result.get(0).scopeId()).isEqualTo(ASSET_ID);
		assertThat(result.get(1).scopeType()).isEqualTo(ResourceType.UNIT);
		assertThat(result.get(1).scopeId()).isEqualTo(UNIT_ID);
		assertThat(result.get(2).scopeType()).isEqualTo(ResourceType.PROPERTY);
		assertThat(result.get(2).scopeId()).isEqualTo(PROP_ID);
		assertThat(result.get(3).scopeType()).isEqualTo(ResourceType.ORG);
		assertThat(result.get(3).scopeId()).isEqualTo(ORG_ID);
	}

	@Test
	void resolve_asset_propScoped_returnsChain() {
		Organization org = Organization.builder().id(ORG_ID).build();
		Prop prop = Prop.builder().id(PROP_ID).organization(org).build();
		Asset asset = Asset.builder().id(ASSET_ID).prop(prop).build();
		when(assetRepository.findByIdWithParents(ASSET_ID)).thenReturn(Optional.of(asset));

		List<ScopeLevel> result = resolver.resolve(ResourceType.ASSET, ASSET_ID, ORG_ID);

		assertThat(result).hasSize(3);
		assertThat(result.get(0).scopeType()).isEqualTo(ResourceType.ASSET);
		assertThat(result.get(0).scopeId()).isEqualTo(ASSET_ID);
		assertThat(result.get(1).scopeType()).isEqualTo(ResourceType.PROPERTY);
		assertThat(result.get(1).scopeId()).isEqualTo(PROP_ID);
		assertThat(result.get(2).scopeType()).isEqualTo(ResourceType.ORG);
		assertThat(result.get(2).scopeId()).isEqualTo(ORG_ID);
	}

	@Test
	void resolve_asset_orgMismatch_returnsEmpty() {
		Organization otherOrg = Organization.builder().id(UUID.randomUUID()).build();
		Prop prop = Prop.builder().id(PROP_ID).organization(otherOrg).build();
		Asset asset = Asset.builder().id(ASSET_ID).prop(prop).build();
		when(assetRepository.findByIdWithParents(ASSET_ID)).thenReturn(Optional.of(asset));

		List<ScopeLevel> result = resolver.resolve(ResourceType.ASSET, ASSET_ID, ORG_ID);

		assertThat(result).isEmpty();
	}

	@Test
	void resolve_asset_notFound_returnsEmpty() {
		when(assetRepository.findByIdWithParents(ASSET_ID)).thenReturn(Optional.empty());

		List<ScopeLevel> result = resolver.resolve(ResourceType.ASSET, ASSET_ID, ORG_ID);

		assertThat(result).isEmpty();
	}
}
