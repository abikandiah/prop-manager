package com.akandiah.propmanager.features.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.akandiah.propmanager.common.permission.AccessEntry;
import com.akandiah.propmanager.common.permission.Actions;
import com.akandiah.propmanager.common.permission.PermissionDomains;
import com.akandiah.propmanager.features.lease.domain.Lease;
import com.akandiah.propmanager.features.lease.domain.LeaseStatus;
import com.akandiah.propmanager.features.lease.domain.LeaseTenant;
import com.akandiah.propmanager.features.lease.domain.LeaseTenantRepository;
import com.akandiah.propmanager.features.lease.domain.LeaseTenantRole;
import com.akandiah.propmanager.features.organization.domain.MemberScope;
import com.akandiah.propmanager.features.organization.domain.MemberScopeRepository;
import com.akandiah.propmanager.features.organization.domain.Membership;
import com.akandiah.propmanager.features.organization.domain.MembershipRepository;
import com.akandiah.propmanager.features.organization.domain.Organization;
import com.akandiah.propmanager.common.permission.ResourceType;
import com.akandiah.propmanager.features.prop.domain.Prop;
import com.akandiah.propmanager.features.prop.domain.PropRepository;
import com.akandiah.propmanager.features.prop.domain.PropertyType;
import com.akandiah.propmanager.features.tenant.domain.Tenant;
import com.akandiah.propmanager.features.unit.domain.Unit;
import com.akandiah.propmanager.features.unit.domain.UnitStatus;
import com.akandiah.propmanager.features.user.domain.User;

@ExtendWith(MockitoExtension.class)
class JwtHydrationServiceTest {

	@Mock
	private MembershipRepository membershipRepository;
	@Mock
	private MemberScopeRepository memberScopeRepository;
	@Mock
	private PropRepository propRepository;
	@Mock
	private LeaseTenantRepository leaseTenantRepository;

	private JwtHydrationService service;

	@BeforeEach
	void setUp() {
		service = new JwtHydrationService(membershipRepository, memberScopeRepository,
				propRepository, leaseTenantRepository);
	}

	@Nested
	class MemberScopeHydration {

		@Test
		void returnsEmptyWhenNoMemberships() {
			UUID userId = UUID.randomUUID();
			when(membershipRepository.findByUserIdWithUserAndOrg(userId)).thenReturn(List.of());
			when(propRepository.findByOwnerIdWithOrganization(userId)).thenReturn(List.of());
			when(leaseTenantRepository.findActiveByUserIdWithLeaseUnitPropOrg(userId)).thenReturn(List.of());

			List<AccessEntry> result = service.hydrate(userId);

			assertThat(result).isEmpty();
		}

		@Test
		void returnsEmptyWhenMembershipsHaveNoScopes() {
			UUID userId = UUID.randomUUID();
			UUID orgId = UUID.randomUUID();
			UUID membershipId = UUID.randomUUID();
			Membership m = membership(membershipId, org(orgId));
			when(membershipRepository.findByUserIdWithUserAndOrg(userId)).thenReturn(List.of(m));
			when(memberScopeRepository.findByMembershipIdIn(List.of(membershipId))).thenReturn(List.of());
			when(propRepository.findByOwnerIdWithOrganization(userId)).thenReturn(List.of());
			when(leaseTenantRepository.findActiveByUserIdWithLeaseUnitPropOrg(userId)).thenReturn(List.of());

			List<AccessEntry> result = service.hydrate(userId);

			assertThat(result).isEmpty();
		}

		@Test
		void orgScopedEntryUsesOrgIdAsScopeId() {
			UUID userId = UUID.randomUUID();
			UUID orgId = UUID.randomUUID();
			UUID membershipId = UUID.randomUUID();
			Organization org = org(orgId);
			Membership m = membership(membershipId, org);
			MemberScope scope = memberScope(m, ResourceType.ORG, orgId, Map.of("l", "cr", "m", "r"));
			when(membershipRepository.findByUserIdWithUserAndOrg(userId)).thenReturn(List.of(m));
			when(memberScopeRepository.findByMembershipIdIn(List.of(membershipId))).thenReturn(List.of(scope));
			when(propRepository.findByOwnerIdWithOrganization(userId)).thenReturn(List.of());
			when(leaseTenantRepository.findActiveByUserIdWithLeaseUnitPropOrg(userId)).thenReturn(List.of());

			List<AccessEntry> result = service.hydrate(userId);

			assertThat(result).hasSize(1);
			AccessEntry entry = result.get(0);
			assertThat(entry.orgId()).isEqualTo(orgId);
			assertThat(entry.scopeType()).isEqualTo(ResourceType.ORG);
			assertThat(entry.scopeId()).isEqualTo(orgId);
			assertThat(entry.permissions()).containsEntry("l", 3); // c=2, r=1
			assertThat(entry.permissions()).containsEntry("m", 1); // r=1
		}

		@Test
		void propertyScopedEntryUsesScopeId() {
			UUID userId = UUID.randomUUID();
			UUID orgId = UUID.randomUUID();
			UUID membershipId = UUID.randomUUID();
			UUID propId = UUID.randomUUID();
			Organization org = org(orgId);
			Membership m = membership(membershipId, org);
			MemberScope scope = memberScope(m, ResourceType.PROPERTY, propId, Map.of("l", "rcud"));
			when(membershipRepository.findByUserIdWithUserAndOrg(userId)).thenReturn(List.of(m));
			when(memberScopeRepository.findByMembershipIdIn(List.of(membershipId))).thenReturn(List.of(scope));
			when(propRepository.findByOwnerIdWithOrganization(userId)).thenReturn(List.of());
			when(leaseTenantRepository.findActiveByUserIdWithLeaseUnitPropOrg(userId)).thenReturn(List.of());

			List<AccessEntry> result = service.hydrate(userId);

			assertThat(result).hasSize(1);
			AccessEntry entry = result.get(0);
			assertThat(entry.scopeType()).isEqualTo(ResourceType.PROPERTY);
			assertThat(entry.scopeId()).isEqualTo(propId);
			assertThat(entry.permissions()).containsEntry("l", 15);
		}

		@Test
		void twoScopesProduceTwoEntries() {
			UUID userId = UUID.randomUUID();
			UUID orgId = UUID.randomUUID();
			UUID membershipId = UUID.randomUUID();
			UUID propId1 = UUID.randomUUID();
			UUID propId2 = UUID.randomUUID();
			Organization org = org(orgId);
			Membership m = membership(membershipId, org);
			MemberScope scope1 = memberScope(m, ResourceType.PROPERTY, propId1, Map.of("l", "r"));
			MemberScope scope2 = memberScope(m, ResourceType.PROPERTY, propId2, Map.of("m", "cru"));
			when(membershipRepository.findByUserIdWithUserAndOrg(userId)).thenReturn(List.of(m));
			when(memberScopeRepository.findByMembershipIdIn(List.of(membershipId)))
					.thenReturn(List.of(scope1, scope2));
			when(propRepository.findByOwnerIdWithOrganization(userId)).thenReturn(List.of());
			when(leaseTenantRepository.findActiveByUserIdWithLeaseUnitPropOrg(userId)).thenReturn(List.of());

			List<AccessEntry> result = service.hydrate(userId);

			assertThat(result).hasSize(2);
		}
	}

	@Nested
	class PropertyOwnerHydration {

		@Test
		void ownedPropProducesPropertyEntryWithFullCrud() {
			UUID userId = UUID.randomUUID();
			UUID orgId = UUID.randomUUID();
			UUID propId = UUID.randomUUID();
			Organization org = org(orgId);
			Prop prop = prop(propId, org, userId);
			when(membershipRepository.findByUserIdWithUserAndOrg(userId)).thenReturn(List.of());
			when(propRepository.findByOwnerIdWithOrganization(userId)).thenReturn(List.of(prop));
			when(leaseTenantRepository.findActiveByUserIdWithLeaseUnitPropOrg(userId)).thenReturn(List.of());

			List<AccessEntry> result = service.hydrate(userId);

			assertThat(result).hasSize(1);
			AccessEntry entry = result.get(0);
			assertThat(entry.orgId()).isEqualTo(orgId);
			assertThat(entry.scopeType()).isEqualTo(ResourceType.PROPERTY);
			assertThat(entry.scopeId()).isEqualTo(propId);
			int fullCrud = Actions.READ | Actions.CREATE | Actions.UPDATE | Actions.DELETE;
			assertThat(entry.permissions()).containsEntry(PermissionDomains.LEASES, fullCrud);
			assertThat(entry.permissions()).containsEntry(PermissionDomains.MAINTENANCE, fullCrud);
			assertThat(entry.permissions()).containsEntry(PermissionDomains.FINANCES, fullCrud);
			assertThat(entry.permissions()).containsEntry(PermissionDomains.TENANTS, fullCrud);
		}

		@Test
		void multipleOwnedPropsProduceMultipleEntries() {
			UUID userId = UUID.randomUUID();
			UUID orgId = UUID.randomUUID();
			Organization org = org(orgId);
			Prop prop1 = prop(UUID.randomUUID(), org, userId);
			Prop prop2 = prop(UUID.randomUUID(), org, userId);
			when(membershipRepository.findByUserIdWithUserAndOrg(userId)).thenReturn(List.of());
			when(propRepository.findByOwnerIdWithOrganization(userId)).thenReturn(List.of(prop1, prop2));
			when(leaseTenantRepository.findActiveByUserIdWithLeaseUnitPropOrg(userId)).thenReturn(List.of());

			List<AccessEntry> result = service.hydrate(userId);

			assertThat(result).hasSize(2);
			assertThat(result).extracting(AccessEntry::scopeId)
					.containsExactlyInAnyOrder(prop1.getId(), prop2.getId());
		}
	}

	@Nested
	class LeaseTenantHydration {

		@Test
		void activeLeaseTenantProducesUnitEntryWithReadOnLeases() {
			UUID userId = UUID.randomUUID();
			UUID orgId = UUID.randomUUID();
			UUID unitId = UUID.randomUUID();
			Organization org = org(orgId);
			Prop prop = prop(UUID.randomUUID(), org, UUID.randomUUID());
			Unit unit = unit(unitId, prop);
			Lease lease = lease(unit, prop, LeaseStatus.ACTIVE);
			LeaseTenant lt = leaseTenant(lease, userId);
			when(membershipRepository.findByUserIdWithUserAndOrg(userId)).thenReturn(List.of());
			when(propRepository.findByOwnerIdWithOrganization(userId)).thenReturn(List.of());
			when(leaseTenantRepository.findActiveByUserIdWithLeaseUnitPropOrg(userId)).thenReturn(List.of(lt));

			List<AccessEntry> result = service.hydrate(userId);

			assertThat(result).hasSize(1);
			AccessEntry entry = result.get(0);
			assertThat(entry.orgId()).isEqualTo(orgId);
			assertThat(entry.scopeType()).isEqualTo(ResourceType.UNIT);
			assertThat(entry.scopeId()).isEqualTo(unitId);
			assertThat(entry.permissions()).containsEntry(PermissionDomains.LEASES, Actions.READ);
			assertThat(entry.permissions()).doesNotContainKey(PermissionDomains.MAINTENANCE);
			assertThat(entry.permissions()).doesNotContainKey(PermissionDomains.FINANCES);
			assertThat(entry.permissions()).doesNotContainKey(PermissionDomains.TENANTS);
		}

		@Test
		void reviewLeaseTenantAlsoGetsAccess() {
			UUID userId = UUID.randomUUID();
			UUID orgId = UUID.randomUUID();
			Organization org = org(orgId);
			Prop prop = prop(UUID.randomUUID(), org, UUID.randomUUID());
			Unit unit = unit(UUID.randomUUID(), prop);
			Lease lease = lease(unit, prop, LeaseStatus.REVIEW);
			LeaseTenant lt = leaseTenant(lease, userId);
			when(membershipRepository.findByUserIdWithUserAndOrg(userId)).thenReturn(List.of());
			when(propRepository.findByOwnerIdWithOrganization(userId)).thenReturn(List.of());
			when(leaseTenantRepository.findActiveByUserIdWithLeaseUnitPropOrg(userId)).thenReturn(List.of(lt));

			List<AccessEntry> result = service.hydrate(userId);

			assertThat(result).hasSize(1);
			assertThat(result.get(0).scopeType()).isEqualTo(ResourceType.UNIT);
		}
	}

	@Nested
	class Deduplication {

		@Test
		void mergesSameOrgResourceTypeScopeIdByOringBitmasks() {
			AccessEntry a = new AccessEntry(
					UUID.randomUUID(), ResourceType.PROPERTY, UUID.randomUUID(), Map.of("l", 1));
			AccessEntry b = new AccessEntry(
					a.orgId(), ResourceType.PROPERTY, a.scopeId(), Map.of("l", 6, "m", 3));

			List<AccessEntry> result = JwtHydrationService.deduplicateAccess(List.of(a, b));

			assertThat(result).hasSize(1);
			AccessEntry merged = result.get(0);
			assertThat(merged.permissions()).containsEntry("l", 7); // 1 | 6 = 7
			assertThat(merged.permissions()).containsEntry("m", 3);
		}

		@Test
		void differentScopeIdsRemainSeparate() {
			UUID orgId = UUID.randomUUID();
			AccessEntry a = new AccessEntry(orgId, ResourceType.PROPERTY, UUID.randomUUID(), Map.of("l", 1));
			AccessEntry b = new AccessEntry(orgId, ResourceType.PROPERTY, UUID.randomUUID(), Map.of("l", 2));

			List<AccessEntry> result = JwtHydrationService.deduplicateAccess(List.of(a, b));

			assertThat(result).hasSize(2);
		}

		@Test
		void ownerAndMemberScopeMergedForSameProperty() {
			UUID userId = UUID.randomUUID();
			UUID orgId = UUID.randomUUID();
			UUID propId = UUID.randomUUID();
			UUID membershipId = UUID.randomUUID();
			Organization org = org(orgId);
			Membership m = membership(membershipId, org);
			MemberScope scope = memberScope(m, ResourceType.PROPERTY, propId, Map.of("l", "r"));
			Prop prop = prop(propId, org, userId);
			when(membershipRepository.findByUserIdWithUserAndOrg(userId)).thenReturn(List.of(m));
			when(memberScopeRepository.findByMembershipIdIn(List.of(membershipId))).thenReturn(List.of(scope));
			when(propRepository.findByOwnerIdWithOrganization(userId)).thenReturn(List.of(prop));
			when(leaseTenantRepository.findActiveByUserIdWithLeaseUnitPropOrg(userId)).thenReturn(List.of());

			List<AccessEntry> result = service.hydrate(userId);

			assertThat(result).hasSize(1);
			AccessEntry entry = result.get(0);
			int fullCrud = Actions.READ | Actions.CREATE | Actions.UPDATE | Actions.DELETE;
			assertThat(entry.permissions()).containsEntry(PermissionDomains.LEASES, fullCrud);
			assertThat(entry.permissions()).containsEntry(PermissionDomains.MAINTENANCE, fullCrud);
		}
	}

	@Test
	void toClaimMapRoundTrip() {
		UUID orgId = UUID.randomUUID();
		UUID scopeId = UUID.randomUUID();
		AccessEntry entry = new AccessEntry(orgId, ResourceType.PROPERTY, scopeId, Map.of("l", 7, "m", 3));

		Map<String, Object> map = entry.toClaimMap();
		AccessEntry back = AccessEntry.fromClaimMap(map);

		assertThat(back).isEqualTo(entry);
	}

	// --- Test data builders ---

	private static Organization org(UUID id) {
		return Organization.builder()
				.id(id)
				.name("Test Org")
				.version(0)
				.createdAt(Instant.now())
				.updatedAt(Instant.now())
				.build();
	}

	private static Membership membership(UUID id, Organization org) {
		User user = User.builder()
				.id(UUID.randomUUID())
				.name("User")
				.email("u@example.com")
				.build();
		return Membership.builder()
				.id(id)
				.user(user)
				.organization(org)
				.version(0)
				.createdAt(Instant.now())
				.updatedAt(Instant.now())
				.build();
	}

	private static MemberScope memberScope(Membership membership, ResourceType scopeType, UUID scopeId,
			Map<String, String> permissions) {
		return MemberScope.builder()
				.id(UUID.randomUUID())
				.membership(membership)
				.scopeType(scopeType)
				.scopeId(scopeId)
				.permissions(permissions)
				.version(0)
				.createdAt(Instant.now())
				.updatedAt(Instant.now())
				.build();
	}

	private static Prop prop(UUID id, Organization org, UUID ownerId) {
		return Prop.builder()
				.id(id)
				.legalName("Test Property")
				.propertyType(PropertyType.SINGLE_FAMILY_HOME)
				.organization(org)
				.ownerId(ownerId)
				.version(0)
				.createdAt(Instant.now())
				.updatedAt(Instant.now())
				.build();
	}

	private static Unit unit(UUID id, Prop prop) {
		return Unit.builder()
				.id(id)
				.prop(prop)
				.unitNumber("101")
				.status(UnitStatus.VACANT)
				.version(0)
				.createdAt(Instant.now())
				.updatedAt(Instant.now())
				.build();
	}

	private static Lease lease(Unit unit, Prop prop, LeaseStatus status) {
		return Lease.builder()
				.id(UUID.randomUUID())
				.unit(unit)
				.property(prop)
				.status(status)
				.startDate(java.time.LocalDate.now())
				.endDate(java.time.LocalDate.now().plusYears(1))
				.rentAmount(java.math.BigDecimal.valueOf(1500))
				.rentDueDay(1)
				.version(0)
				.createdAt(Instant.now())
				.updatedAt(Instant.now())
				.build();
	}

	private static LeaseTenant leaseTenant(Lease lease, UUID userId) {
		User user = User.builder()
				.id(userId)
				.name("Tenant User")
				.email("tenant@example.com")
				.build();
		Tenant tenant = Tenant.builder()
				.id(UUID.randomUUID())
				.user(user)
				.version(0)
				.createdAt(Instant.now())
				.updatedAt(Instant.now())
				.build();
		return LeaseTenant.builder()
				.id(UUID.randomUUID())
				.lease(lease)
				.tenant(tenant)
				.role(LeaseTenantRole.PRIMARY)
				.version(0)
				.createdAt(Instant.now())
				.updatedAt(Instant.now())
				.build();
	}
}
