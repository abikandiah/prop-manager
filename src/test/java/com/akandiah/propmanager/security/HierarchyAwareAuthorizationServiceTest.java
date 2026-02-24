package com.akandiah.propmanager.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.akandiah.propmanager.common.permission.AccessEntry;
import com.akandiah.propmanager.common.permission.Actions;
import com.akandiah.propmanager.common.permission.HierarchyResolver;
import com.akandiah.propmanager.common.permission.PermissionDomains;
import com.akandiah.propmanager.common.permission.ResourceType;
import com.akandiah.propmanager.common.permission.ScopeLevel;

@ExtendWith(MockitoExtension.class)
class HierarchyAwareAuthorizationServiceTest {

	@Mock
	private HierarchyResolver hierarchyResolver;

	private HierarchyAwareAuthorizationService service;

	private static final UUID ORG_ID = UUID.randomUUID();
	private static final UUID PROP_ID = UUID.randomUUID();
	private static final UUID UNIT_ID = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		service = new HierarchyAwareAuthorizationService(hierarchyResolver);
	}

	@Test
	void allow_returnsFalseWhenAccessEmpty() {
		boolean result = service.allow(List.of(), Actions.READ, PermissionDomains.LEASES,
				ResourceType.ORG, ORG_ID, ORG_ID);

		assertThat(result).isFalse();
	}

	@Test
	void allow_returnsFalseWhenAccessNull() {
		boolean result = service.allow(null, Actions.READ, PermissionDomains.LEASES,
				ResourceType.ORG, ORG_ID, ORG_ID);

		assertThat(result).isFalse();
	}

	@Test
	void allow_returnsTrueWhenOrgEntryGrantsAction() {
		when(hierarchyResolver.resolve(ResourceType.ORG, ORG_ID, ORG_ID))
				.thenReturn(List.of(new ScopeLevel(ResourceType.ORG, ORG_ID)));
		List<AccessEntry> access = List.of(
				new AccessEntry(ORG_ID, ResourceType.ORG, ORG_ID, Map.of(PermissionDomains.LEASES, 1))); // READ

		boolean result = service.allow(access, Actions.READ, PermissionDomains.LEASES,
				ResourceType.ORG, ORG_ID, ORG_ID);

		assertThat(result).isTrue();
	}

	@Test
	void allow_returnsFalseWhenOrgEntryDoesNotGrantAction() {
		when(hierarchyResolver.resolve(ResourceType.ORG, ORG_ID, ORG_ID))
				.thenReturn(List.of(new ScopeLevel(ResourceType.ORG, ORG_ID)));
		List<AccessEntry> access = List.of(
				new AccessEntry(ORG_ID, ResourceType.ORG, ORG_ID, Map.of(PermissionDomains.LEASES, 1))); // READ only

		boolean result = service.allow(access, Actions.UPDATE, PermissionDomains.LEASES,
				ResourceType.ORG, ORG_ID, ORG_ID);

		assertThat(result).isFalse();
	}

	@Test
	void allow_returnsTrueWhenScopeInChainGrantsAction() {
		when(hierarchyResolver.resolve(ResourceType.UNIT, UNIT_ID, ORG_ID))
				.thenReturn(List.of(
						new ScopeLevel(ResourceType.UNIT, UNIT_ID),
						new ScopeLevel(ResourceType.PROPERTY, PROP_ID),
						new ScopeLevel(ResourceType.ORG, ORG_ID)));
		// No UNIT entry; PROPERTY grants READ
		List<AccessEntry> access = List.of(
				new AccessEntry(ORG_ID, ResourceType.ORG, ORG_ID, Map.of(PermissionDomains.LEASES, 1)),
				new AccessEntry(ORG_ID, ResourceType.PROPERTY, PROP_ID, Map.of(PermissionDomains.LEASES, 7))); // r+c+u

		boolean result = service.allow(access, Actions.READ, PermissionDomains.LEASES,
				ResourceType.UNIT, UNIT_ID, ORG_ID);

		assertThat(result).isTrue();
	}

	@Test
	void allow_returnsFalseWhenResolverReturnsEmpty() {
		when(hierarchyResolver.resolve(ResourceType.PROPERTY, PROP_ID, ORG_ID))
				.thenReturn(List.of());
		List<AccessEntry> access = List.of(
				new AccessEntry(ORG_ID, ResourceType.PROPERTY, PROP_ID, Map.of(PermissionDomains.LEASES, 15)));

		boolean result = service.allow(access, Actions.READ, PermissionDomains.LEASES,
				ResourceType.PROPERTY, PROP_ID, ORG_ID);

		assertThat(result).isFalse();
	}
}
