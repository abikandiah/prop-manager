package com.akandiah.propmanager.features.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.akandiah.propmanager.common.permission.AccessEntry;
import com.akandiah.propmanager.features.organization.domain.MemberScope;
import com.akandiah.propmanager.features.organization.domain.MemberScopeRepository;
import com.akandiah.propmanager.features.organization.domain.Membership;
import com.akandiah.propmanager.features.organization.domain.MembershipRepository;
import com.akandiah.propmanager.features.organization.domain.Organization;
import com.akandiah.propmanager.features.organization.domain.ScopeType;
import com.akandiah.propmanager.features.user.domain.User;

@ExtendWith(MockitoExtension.class)
class JwtHydrationServiceTest {

	@Mock
	private MembershipRepository membershipRepository;
	@Mock
	private MemberScopeRepository memberScopeRepository;

	private JwtHydrationService service;

	@BeforeEach
	void setUp() {
		service = new JwtHydrationService(membershipRepository, memberScopeRepository);
	}

	@Test
	void hydrate_returnsEmptyWhenNoMemberships() {
		UUID userId = UUID.randomUUID();
		when(membershipRepository.findByUserIdWithUserAndOrg(userId)).thenReturn(List.of());

		List<AccessEntry> result = service.hydrate(userId);

		assertThat(result).isEmpty();
	}

	@Test
	void hydrate_returnsEmptyWhenMembershipsHaveNoScopes() {
		UUID userId = UUID.randomUUID();
		UUID orgId = UUID.randomUUID();
		UUID membershipId = UUID.randomUUID();
		Membership m = membership(membershipId, org(orgId));
		when(membershipRepository.findByUserIdWithUserAndOrg(userId)).thenReturn(List.of(m));
		when(memberScopeRepository.findByMembershipIdIn(List.of(membershipId))).thenReturn(List.of());

		List<AccessEntry> result = service.hydrate(userId);

		assertThat(result).isEmpty();
	}

	@Test
	void hydrate_orgScopedEntryUsesOrgIdAsScopeId() {
		UUID userId = UUID.randomUUID();
		UUID orgId = UUID.randomUUID();
		UUID membershipId = UUID.randomUUID();
		Organization org = org(orgId);
		Membership m = membership(membershipId, org);
		MemberScope scope = memberScope(m, ScopeType.ORG, orgId, Map.of("l", "cr", "m", "r"));
		when(membershipRepository.findByUserIdWithUserAndOrg(userId)).thenReturn(List.of(m));
		when(memberScopeRepository.findByMembershipIdIn(List.of(membershipId))).thenReturn(List.of(scope));

		List<AccessEntry> result = service.hydrate(userId);

		assertThat(result).hasSize(1);
		AccessEntry entry = result.get(0);
		assertThat(entry.orgId()).isEqualTo(orgId);
		assertThat(entry.scopeType()).isEqualTo("ORG");
		assertThat(entry.scopeId()).isEqualTo(orgId);
		assertThat(entry.permissions()).containsEntry("l", 3); // c=2, r=1
		assertThat(entry.permissions()).containsEntry("m", 1); // r=1
	}

	@Test
	void hydrate_propertyScopedEntryUsesScopeId() {
		UUID userId = UUID.randomUUID();
		UUID orgId = UUID.randomUUID();
		UUID membershipId = UUID.randomUUID();
		UUID propId = UUID.randomUUID();
		Organization org = org(orgId);
		Membership m = membership(membershipId, org);
		MemberScope scope = memberScope(m, ScopeType.PROPERTY, propId, Map.of("l", "rcud"));
		when(membershipRepository.findByUserIdWithUserAndOrg(userId)).thenReturn(List.of(m));
		when(memberScopeRepository.findByMembershipIdIn(List.of(membershipId))).thenReturn(List.of(scope));

		List<AccessEntry> result = service.hydrate(userId);

		assertThat(result).hasSize(1);
		AccessEntry entry = result.get(0);
		assertThat(entry.scopeType()).isEqualTo("PROPERTY");
		assertThat(entry.scopeId()).isEqualTo(propId);
		assertThat(entry.permissions()).containsEntry("l", 15); // r=1,c=2,u=4,d=8
	}

	@Test
	void hydrate_twoScopesProduceTwoEntries() {
		UUID userId = UUID.randomUUID();
		UUID orgId = UUID.randomUUID();
		UUID membershipId = UUID.randomUUID();
		UUID propId1 = UUID.randomUUID();
		UUID propId2 = UUID.randomUUID();
		Organization org = org(orgId);
		Membership m = membership(membershipId, org);
		MemberScope scope1 = memberScope(m, ScopeType.PROPERTY, propId1, Map.of("l", "r"));
		MemberScope scope2 = memberScope(m, ScopeType.PROPERTY, propId2, Map.of("m", "cru"));
		when(membershipRepository.findByUserIdWithUserAndOrg(userId)).thenReturn(List.of(m));
		when(memberScopeRepository.findByMembershipIdIn(List.of(membershipId))).thenReturn(List.of(scope1, scope2));

		List<AccessEntry> result = service.hydrate(userId);

		assertThat(result).hasSize(2);
	}

	@Test
	void hydrate_toClaimMapRoundTrip() {
		UUID orgId = UUID.randomUUID();
		UUID scopeId = UUID.randomUUID();
		AccessEntry entry = new AccessEntry(orgId, "PROPERTY", scopeId, Map.of("l", 7, "m", 3));

		Map<String, Object> map = entry.toClaimMap();
		AccessEntry back = AccessEntry.fromClaimMap(map);

		assertThat(back).isEqualTo(entry);
	}

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

	private static MemberScope memberScope(Membership membership, ScopeType scopeType, UUID scopeId,
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
}
