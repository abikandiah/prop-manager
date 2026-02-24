package com.akandiah.propmanager.features.lease.service;

import static com.akandiah.propmanager.TestDataFactory.lease;
import static com.akandiah.propmanager.TestDataFactory.unit;
import static com.akandiah.propmanager.TestDataFactory.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import com.akandiah.propmanager.features.lease.domain.Lease;
import com.akandiah.propmanager.features.lease.domain.LeaseRepository;
import com.akandiah.propmanager.features.lease.domain.LeaseTenantRepository;
import com.akandiah.propmanager.features.prop.domain.PropRepository;
import com.akandiah.propmanager.features.unit.domain.Unit;
import com.akandiah.propmanager.features.unit.domain.UnitRepository;
import com.akandiah.propmanager.features.user.domain.User;
import com.akandiah.propmanager.features.user.service.UserService;

/**
 * Tests the tenant isolation filter in {@link LeaseService#findById(UUID)}.
 *
 * <p>
 * Design decision 5: Tenants have UNIT-scope READ access on `l`, but the
 * service
 * applies an additional application-level check so a tenant can only read their
 * own
 * lease, not a co-tenant's lease on the same unit.
 */
@ExtendWith(MockitoExtension.class)
class LeaseServiceTenantFilterTest {

	@Mock
	LeaseRepository leaseRepository;
	@Mock
	LeaseTemplateService templateService;
	@Mock
	UnitRepository unitRepository;
	@Mock
	PropRepository propRepository;
	@Mock
	LeaseTenantRepository leaseTenantRepository;
	@Mock
	LeaseStateMachine stateMachine;
	@Mock
	LeaseTemplateRenderer renderer;
	@Mock
	ApplicationEventPublisher eventPublisher;
	@Mock
	UserService userService;

	LeaseService service;

	private static final UUID UNIT_ID = UUID.randomUUID();
	private static final UUID LEASE_A_ID = UUID.randomUUID();
	private static final UUID LEASE_B_ID = UUID.randomUUID();
	private static final UUID TENANT_A_USER_ID = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		service = new LeaseService(leaseRepository, templateService,
				unitRepository, propRepository, leaseTenantRepository,
				stateMachine, renderer, eventPublisher, userService);
		// Clear security context so tests start clean
		SecurityContextHolder.clearContext();
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void findById_returnsLeaseWhenCallerIsNotATenant() {
		Unit unit = unit().id(UNIT_ID).build();
		Lease leaseA = lease().id(LEASE_A_ID).unit(unit).build();

		when(leaseRepository.findById(LEASE_A_ID)).thenReturn(Optional.of(leaseA));
		// No SecurityContext set → getCurrentUserId() returns empty → no filter applied

		var result = service.findById(LEASE_A_ID);

		assertThat(result.id()).isEqualTo(LEASE_A_ID);
	}

	@Test
	void findById_allowsTenantToReadOwnLease() {
		Unit unit = unit().id(UNIT_ID).build();
		Lease leaseA = lease().id(LEASE_A_ID).unit(unit).build();
		User tenantAUser = user().id(TENANT_A_USER_ID).build();

		when(leaseRepository.findById(LEASE_A_ID)).thenReturn(Optional.of(leaseA));
		// Simulate a JWT auth context
		mockJwtContext(tenantAUser);
		// Tenant A is on the unit
		when(leaseTenantRepository.existsByLease_Unit_IdAndTenant_User_Id(UNIT_ID, TENANT_A_USER_ID))
				.thenReturn(true);
		// Tenant A is on lease A specifically
		when(leaseTenantRepository.existsByLease_IdAndTenant_User_Id(LEASE_A_ID, TENANT_A_USER_ID))
				.thenReturn(true);

		var result = service.findById(LEASE_A_ID);

		assertThat(result.id()).isEqualTo(LEASE_A_ID);
	}

	@Test
	void findById_deniesAccessWhenTenantTriesToReadCoTenantsLease() {
		Unit unit = unit().id(UNIT_ID).build();
		Lease leaseB = lease().id(LEASE_B_ID).unit(unit).build();
		User tenantAUser = user().id(TENANT_A_USER_ID).build();

		when(leaseRepository.findById(LEASE_B_ID)).thenReturn(Optional.of(leaseB));
		mockJwtContext(tenantAUser);
		// Tenant A is on the unit (via a different lease)
		when(leaseTenantRepository.existsByLease_Unit_IdAndTenant_User_Id(UNIT_ID, TENANT_A_USER_ID))
				.thenReturn(true);
		// Tenant A is NOT on lease B
		when(leaseTenantRepository.existsByLease_IdAndTenant_User_Id(LEASE_B_ID, TENANT_A_USER_ID))
				.thenReturn(false);

		assertThatThrownBy(() -> service.findById(LEASE_B_ID))
				.isInstanceOf(AccessDeniedException.class)
				.hasMessageContaining("Tenants can only access their own lease");
	}

	// ─────────────────────────── Helper ───────────────────────────

	/**
	 * Installs a minimal JWT-backed SecurityContext so {@code getCurrentUserId()}
	 * resolves to the given user's ID.
	 */
	private void mockJwtContext(User user) {
		var jwt = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("test")
				.header("alg", "RS256")
				.claim("sub", user.getId().toString())
				.build();
		var auth = new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(jwt);
		SecurityContextHolder.getContext().setAuthentication(auth);
		when(userService.findUserFromJwt(jwt)).thenReturn(Optional.of(user));
	}
}
