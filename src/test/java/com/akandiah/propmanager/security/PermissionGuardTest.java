package com.akandiah.propmanager.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.akandiah.propmanager.common.permission.Actions;
import com.akandiah.propmanager.common.permission.PermissionDomains;
import com.akandiah.propmanager.common.permission.ResourceType;
import com.akandiah.propmanager.features.lease.domain.LeaseTenantRepository;
import com.akandiah.propmanager.features.lease.domain.LeaseRepository;

@ExtendWith(MockitoExtension.class)
class PermissionGuardTest {

	@Mock
	private HierarchyAwareAuthorizationService authorizationService;
	@Mock
	private LeaseRepository leaseRepository;
	@Mock
	private LeaseTenantRepository leaseTenantRepository;

	private PermissionGuard guard;

	private static final UUID ORG_ID = UUID.randomUUID();
	private static final UUID ASSET_ID = UUID.randomUUID();
	private static final UUID PROP_ID = UUID.randomUUID();
	private static final UUID UNIT_ID = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		guard = new PermissionGuard(authorizationService, leaseRepository, leaseTenantRepository);
		// Set up a non-admin security context and a request with an empty access list
		SecurityContextHolder.getContext().setAuthentication(
				new TestingAuthenticationToken("user", null, List.of()));
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(JwtAccessHydrationFilter.REQUEST_ATTRIBUTE_ACCESS, List.of());
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
		RequestContextHolder.resetRequestAttributes();
	}

	// ─────────────────── hasTenantAccess ───────────────────

	private static final UUID TENANT_ID = UUID.randomUUID();

	@Test
	void hasTenantAccess_returnsTrueForGlobalAdmin() {
		SecurityContextHolder.getContext().setAuthentication(
				new TestingAuthenticationToken("admin", null,
						List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

		boolean result = guard.hasTenantAccess(Actions.READ, PermissionDomains.LEASES, TENANT_ID, ORG_ID);

		assertThat(result).isTrue();
	}

	@Test
	void hasTenantAccess_returnsFalseWhenTenantHasNoActiveLeases() {
		when(leaseTenantRepository.findUnitIdsByTenantId(TENANT_ID)).thenReturn(List.of());

		boolean result = guard.hasTenantAccess(Actions.READ, PermissionDomains.LEASES, TENANT_ID, ORG_ID);

		assertThat(result).isFalse();
	}

	@Test
	void hasTenantAccess_returnsTrueWhenCallerHasAccessToTenantUnit() {
		when(leaseTenantRepository.findUnitIdsByTenantId(TENANT_ID)).thenReturn(List.of(UNIT_ID));
		when(authorizationService.allow(any(), eq(Actions.READ), eq(PermissionDomains.LEASES),
				eq(ResourceType.UNIT), eq(UNIT_ID), eq(ORG_ID))).thenReturn(true);

		boolean result = guard.hasTenantAccess(Actions.READ, PermissionDomains.LEASES, TENANT_ID, ORG_ID);

		assertThat(result).isTrue();
		verify(authorizationService).allow(any(), eq(Actions.READ), eq(PermissionDomains.LEASES),
				eq(ResourceType.UNIT), eq(UNIT_ID), eq(ORG_ID));
	}

	@Test
	void hasTenantAccess_returnsFalseWhenCallerLacksAccessToAllTenantUnits() {
		UUID unit2 = UUID.randomUUID();
		when(leaseTenantRepository.findUnitIdsByTenantId(TENANT_ID)).thenReturn(List.of(UNIT_ID, unit2));
		when(authorizationService.allow(any(), eq(Actions.READ), eq(PermissionDomains.LEASES),
				eq(ResourceType.UNIT), any(), eq(ORG_ID))).thenReturn(false);

		boolean result = guard.hasTenantAccess(Actions.READ, PermissionDomains.LEASES, TENANT_ID, ORG_ID);

		assertThat(result).isFalse();
	}

	// ─────────────────── hasAssetAccess ───────────────────

	@Test
	void hasAssetAccess_delegatesToResourceTypeAsset() {
		when(authorizationService.allow(any(), eq(Actions.READ), eq(PermissionDomains.MAINTENANCE),
				eq(ResourceType.ASSET), eq(ASSET_ID), eq(ORG_ID))).thenReturn(true);

		boolean result = guard.hasAssetAccess(Actions.READ, PermissionDomains.MAINTENANCE, ASSET_ID, ORG_ID);

		assertThat(result).isTrue();
		verify(authorizationService).allow(any(), eq(Actions.READ), eq(PermissionDomains.MAINTENANCE),
				eq(ResourceType.ASSET), eq(ASSET_ID), eq(ORG_ID));
	}

	@Test
	void hasAssetAccess_returnsTrueForGlobalAdmin() {
		SecurityContextHolder.getContext().setAuthentication(
				new TestingAuthenticationToken("admin", null,
						List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

		boolean result = guard.hasAssetAccess(Actions.DELETE, PermissionDomains.MAINTENANCE, ASSET_ID, ORG_ID);

		assertThat(result).isTrue();
	}

	// ─────────────────── hasAssetCreateAccess ───────────────────

	@Test
	void hasAssetCreateAccess_checksPropertyScopeWhenPropertyIdProvided() {
		when(authorizationService.allow(any(), eq(Actions.CREATE), eq(PermissionDomains.MAINTENANCE),
				eq(ResourceType.PROPERTY), eq(PROP_ID), eq(ORG_ID))).thenReturn(true);

		boolean result = guard.hasAssetCreateAccess(
				Actions.CREATE, PermissionDomains.MAINTENANCE, PROP_ID, null, ORG_ID);

		assertThat(result).isTrue();
		verify(authorizationService).allow(any(), eq(Actions.CREATE), eq(PermissionDomains.MAINTENANCE),
				eq(ResourceType.PROPERTY), eq(PROP_ID), eq(ORG_ID));
	}

	@Test
	void hasAssetCreateAccess_checksUnitScopeWhenUnitIdProvided() {
		when(authorizationService.allow(any(), eq(Actions.CREATE), eq(PermissionDomains.MAINTENANCE),
				eq(ResourceType.UNIT), eq(UNIT_ID), eq(ORG_ID))).thenReturn(true);

		boolean result = guard.hasAssetCreateAccess(
				Actions.CREATE, PermissionDomains.MAINTENANCE, null, UNIT_ID, ORG_ID);

		assertThat(result).isTrue();
		verify(authorizationService).allow(any(), eq(Actions.CREATE), eq(PermissionDomains.MAINTENANCE),
				eq(ResourceType.UNIT), eq(UNIT_ID), eq(ORG_ID));
	}

	@Test
	void hasAssetCreateAccess_propertyIdTakesPrecedenceOverUnitId() {
		when(authorizationService.allow(any(), eq(Actions.CREATE), eq(PermissionDomains.MAINTENANCE),
				eq(ResourceType.PROPERTY), eq(PROP_ID), eq(ORG_ID))).thenReturn(true);

		// Both provided — property wins
		boolean result = guard.hasAssetCreateAccess(
				Actions.CREATE, PermissionDomains.MAINTENANCE, PROP_ID, UNIT_ID, ORG_ID);

		assertThat(result).isTrue();
		verify(authorizationService).allow(any(), eq(Actions.CREATE), eq(PermissionDomains.MAINTENANCE),
				eq(ResourceType.PROPERTY), eq(PROP_ID), eq(ORG_ID));
	}

	@Test
	void hasAssetCreateAccess_returnsTrueForGlobalAdmin() {
		SecurityContextHolder.getContext().setAuthentication(
				new TestingAuthenticationToken("admin", null,
						List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

		boolean result = guard.hasAssetCreateAccess(
				Actions.CREATE, PermissionDomains.MAINTENANCE, PROP_ID, null, ORG_ID);

		assertThat(result).isTrue();
	}

	// ─────────────────── String-based Overloads ───────────────────

	@Test
	void hasAccess_stringOverload_mapsStringsCorrectly() {
		UUID resourceId = UUID.randomUUID();
		when(authorizationService.allow(any(), eq(Actions.READ), eq(PermissionDomains.PORTFOLIO),
				eq(ResourceType.PROPERTY), eq(resourceId), eq(ORG_ID))).thenReturn(true);

		boolean result = guard.hasAccess("READ", "PORTFOLIO", "PROPERTY", resourceId, ORG_ID);

		assertThat(result).isTrue();
		verify(authorizationService).allow(any(), eq(Actions.READ), eq(PermissionDomains.PORTFOLIO),
				eq(ResourceType.PROPERTY), eq(resourceId), eq(ORG_ID));
	}

	@Test
	void hasAccess_stringOverload_handlesCaseInsensitivity() {
		UUID resourceId = UUID.randomUUID();
		when(authorizationService.allow(any(), eq(Actions.UPDATE), eq(PermissionDomains.PORTFOLIO),
				eq(ResourceType.UNIT), eq(resourceId), eq(ORG_ID))).thenReturn(true);

		boolean result = guard.hasAccess("update", "portfolio", "unit", resourceId, ORG_ID);

		assertThat(result).isTrue();
	}

	@Test
	void hasAccess_stringOverload_throwsOnInvalidAction() {
		assertThatThrownBy(() -> guard.hasAccess("INVALID", "PORTFOLIO", "PROPERTY", UUID.randomUUID(), ORG_ID))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Invalid security action");
	}

	@Test
	void hasAccess_stringOverload_throwsOnInvalidDomain() {
		assertThatThrownBy(() -> guard.hasAccess("READ", "INVALID", "PROPERTY", UUID.randomUUID(), ORG_ID))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Invalid security domain");
	}

	@Test
	void hasAccess_stringOverload_throwsOnInvalidResourceType() {
		assertThatThrownBy(() -> guard.hasAccess("READ", "PORTFOLIO", "INVALID", UUID.randomUUID(), ORG_ID))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Invalid security resource type");
	}

	@Test
	void hasOrgAccess_stringOverload_mapsCorrectly() {
		when(authorizationService.allow(any(), eq(Actions.CREATE), eq(PermissionDomains.ORGANIZATION),
				eq(ResourceType.ORG), eq(ORG_ID), eq(ORG_ID))).thenReturn(true);

		boolean result = guard.hasOrgAccess("CREATE", "ORG", ORG_ID);

		assertThat(result).isTrue();
	}

	@Test
	void hasLeaseAccess_stringOverload_mapsCorrectly() {
		UUID leaseId = UUID.randomUUID();
		when(leaseRepository.findUnitIdById(leaseId)).thenReturn(java.util.Optional.of(UNIT_ID));
		when(authorizationService.allow(any(), eq(Actions.READ), eq(PermissionDomains.LEASES),
				eq(ResourceType.UNIT), eq(UNIT_ID), eq(ORG_ID))).thenReturn(true);

		boolean result = guard.hasLeaseAccess("READ", "LEASES", leaseId, ORG_ID);

		assertThat(result).isTrue();
	}

	@Test
	void hasAssetAccess_stringOverload_mapsCorrectly() {
		when(authorizationService.allow(any(), eq(Actions.UPDATE), eq(PermissionDomains.MAINTENANCE),
				eq(ResourceType.ASSET), eq(ASSET_ID), eq(ORG_ID))).thenReturn(true);

		boolean result = guard.hasAssetAccess("UPDATE", "MAINTENANCE", ASSET_ID, ORG_ID);

		assertThat(result).isTrue();
	}
}