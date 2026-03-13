package com.akandiah.propmanager.features.tenant.service;

import static com.akandiah.propmanager.TestDataFactory.lease;
import static com.akandiah.propmanager.TestDataFactory.organization;
import static com.akandiah.propmanager.TestDataFactory.prop;
import static com.akandiah.propmanager.TestDataFactory.tenant;
import static com.akandiah.propmanager.TestDataFactory.unit;
import static com.akandiah.propmanager.TestDataFactory.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.features.asset.domain.Asset;
import com.akandiah.propmanager.features.asset.domain.AssetCategory;
import com.akandiah.propmanager.features.asset.domain.AssetRepository;
import com.akandiah.propmanager.features.invite.domain.Invite;
import com.akandiah.propmanager.features.lease.domain.Lease;
import com.akandiah.propmanager.features.lease.domain.LeaseStatus;
import com.akandiah.propmanager.features.lease.domain.LeaseTenant;
import com.akandiah.propmanager.features.lease.domain.LeaseTenantRepository;
import com.akandiah.propmanager.features.lease.domain.LeaseTenantRole;
import com.akandiah.propmanager.features.lease.domain.LeaseTenantStatus;
import com.akandiah.propmanager.features.organization.domain.Organization;
import com.akandiah.propmanager.features.organization.domain.OrganizationRepository;
import com.akandiah.propmanager.features.prop.domain.Prop;
import com.akandiah.propmanager.features.tenant.domain.Tenant;
import com.akandiah.propmanager.features.tenant.domain.TenantRepository;
import com.akandiah.propmanager.features.unit.domain.Unit;
import com.akandiah.propmanager.features.user.domain.User;

@ExtendWith(MockitoExtension.class)
class TenantPortalServiceTest {

	@Mock TenantRepository tenantRepository;
	@Mock LeaseTenantRepository leaseTenantRepository;
	@Mock AssetRepository assetRepository;
	@Mock OrganizationRepository organizationRepository;

	@InjectMocks TenantPortalService service;

	// ──────────────────── helpers ────────────────────

	private Organization makeOrg() {
		return organization().id(UUID.randomUUID()).build();
	}

	private Prop makeProp(Organization org) {
		return prop().id(UUID.randomUUID()).organization(org).build();
	}

	private Unit makeUnit(Prop p) {
		return unit().id(UUID.randomUUID()).prop(p).build();
	}

	private Invite makeInvite(String email) {
		return Invite.builder()
				.id(UUID.randomUUID())
				.email(email)
				.token("tok-" + UUID.randomUUID())
				.targetType(com.akandiah.propmanager.features.invite.domain.TargetType.LEASE)
				.targetId(UUID.randomUUID())
				.invitedBy(user().build())
				.expiresAt(java.time.Instant.now().plusSeconds(86400))
				.build();
	}

	private LeaseTenant makeSlot(Lease l, Tenant t, Invite inv, LeaseTenantRole role, LocalDate signedDate) {
		return LeaseTenant.builder()
				.id(UUID.randomUUID())
				.lease(l)
				.tenant(t)
				.invite(inv)
				.role(role)
				.signedDate(signedDate)
				.build();
	}

	// ──────────────────── tests ────────────────────

	@Test
	void shouldReturn403WhenNotMemberOfOrg() {
		UUID userId = UUID.randomUUID();
		UUID orgId = UUID.randomUUID();
		when(organizationRepository.findAllByUserId(userId)).thenReturn(List.of());

		assertThatThrownBy(() -> service.getMyLease(userId, orgId))
				.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void shouldReturn404WhenNoTenantRecord() {
		Organization org = makeOrg();
		User user = user().build();
		when(organizationRepository.findAllByUserId(user.getId())).thenReturn(List.of(org));
		when(tenantRepository.findByUser_Id(user.getId())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getMyLease(user.getId(), org.getId()))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void shouldReturn404WhenNoLeaseInThisOrg() {
		Organization org = makeOrg();
		User user = user().build();
		Tenant t = tenant().user(user).build();
		when(organizationRepository.findAllByUserId(user.getId())).thenReturn(List.of(org));
		when(tenantRepository.findByUser_Id(user.getId())).thenReturn(Optional.of(t));
		when(leaseTenantRepository.findByTenantIdAndStatusInAndOrgId(eq(t.getId()), any(), eq(org.getId())))
				.thenReturn(List.of());

		assertThatThrownBy(() -> service.getMyLease(user.getId(), org.getId()))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void shouldReturnLeaseViewForActiveLease() {
		Organization org = makeOrg();
		User user = user().build();
		Tenant t = tenant().user(user).build();
		Prop p = makeProp(org);
		Unit u = makeUnit(p);
		Lease l = lease().id(UUID.randomUUID()).status(LeaseStatus.ACTIVE).property(p).unit(u)
				.noticePeriodDays(60).build();
		Invite inv = makeInvite("primary@test.com");
		LeaseTenant slot = makeSlot(l, t, inv, LeaseTenantRole.PRIMARY, LocalDate.now().minusDays(1));

		when(organizationRepository.findAllByUserId(user.getId())).thenReturn(List.of(org));
		when(tenantRepository.findByUser_Id(user.getId())).thenReturn(Optional.of(t));
		when(leaseTenantRepository.findByTenantIdAndStatusInAndOrgId(eq(t.getId()), any(), eq(org.getId())))
				.thenReturn(List.of(slot));
		when(assetRepository.findByUnit_Id(u.getId())).thenReturn(List.of());
		when(leaseTenantRepository.findByLeaseIdWithTenantAndInvite(l.getId())).thenReturn(List.of(slot));

		var result = service.getMyLease(user.getId(), org.getId());

		assertThat(result.lease().id()).isEqualTo(l.getId());
		assertThat(result.lease().status()).isEqualTo(LeaseStatus.ACTIVE);
		assertThat(result.property().id()).isEqualTo(p.getId());
		assertThat(result.unit().id()).isEqualTo(u.getId());
		assertThat(result.coTenants()).isEmpty(); // current tenant is filtered out
	}

	@Test
	void shouldReturnLeaseForDraftReviewSignedStatus() {
		for (LeaseStatus status : List.of(LeaseStatus.DRAFT, LeaseStatus.REVIEW, LeaseStatus.SIGNED)) {
			Organization org = makeOrg();
			User user = user().build();
			Tenant t = tenant().user(user).build();
			Prop p = makeProp(org);
			Unit u = makeUnit(p);
			Lease l = lease().id(UUID.randomUUID()).status(status).property(p).unit(u).build();
			Invite inv = makeInvite("x@test.com");
			LeaseTenant slot = makeSlot(l, t, inv, LeaseTenantRole.PRIMARY, null);

			when(organizationRepository.findAllByUserId(user.getId())).thenReturn(List.of(org));
			when(tenantRepository.findByUser_Id(user.getId())).thenReturn(Optional.of(t));
			when(leaseTenantRepository.findByTenantIdAndStatusInAndOrgId(eq(t.getId()), any(), eq(org.getId())))
					.thenReturn(List.of(slot));
			when(assetRepository.findByUnit_Id(u.getId())).thenReturn(List.of());
			when(leaseTenantRepository.findByLeaseIdWithTenantAndInvite(l.getId())).thenReturn(List.of(slot));

			var result = service.getMyLease(user.getId(), org.getId());

			assertThat(result.lease().status()).isEqualTo(status);
		}
	}

	@Test
	void shouldPreferActiveLease_overDraft() {
		Organization org = makeOrg();
		User user = user().build();
		Tenant t = tenant().user(user).build();
		Prop p = makeProp(org);
		Unit u = makeUnit(p);
		UUID draftId = UUID.randomUUID();
		UUID activeId = UUID.randomUUID();
		Lease draft = lease().id(draftId).status(LeaseStatus.DRAFT).property(p).unit(u)
				.startDate(LocalDate.now().minusMonths(2)).build();
		Lease active = lease().id(activeId).status(LeaseStatus.ACTIVE).property(p).unit(u)
				.startDate(LocalDate.now().minusMonths(1)).build();
		Invite inv = makeInvite("x@test.com");
		LeaseTenant draftSlot = makeSlot(draft, t, inv, LeaseTenantRole.PRIMARY, null);
		LeaseTenant activeSlot = makeSlot(active, t, inv, LeaseTenantRole.PRIMARY, LocalDate.now().minusDays(5));

		when(organizationRepository.findAllByUserId(user.getId())).thenReturn(List.of(org));
		when(tenantRepository.findByUser_Id(user.getId())).thenReturn(Optional.of(t));
		when(leaseTenantRepository.findByTenantIdAndStatusInAndOrgId(eq(t.getId()), any(), eq(org.getId())))
				.thenReturn(List.of(draftSlot, activeSlot));
		when(assetRepository.findByUnit_Id(u.getId())).thenReturn(List.of());
		when(leaseTenantRepository.findByLeaseIdWithTenantAndInvite(activeId)).thenReturn(List.of(activeSlot));

		var result = service.getMyLease(user.getId(), org.getId());

		assertThat(result.lease().id()).isEqualTo(activeId);
	}

	@Test
	void shouldComputeNoticeDeadlineDateCorrectly() {
		Organization org = makeOrg();
		User user = user().build();
		Tenant t = tenant().user(user).build();
		Prop p = makeProp(org);
		Unit u = makeUnit(p);
		LocalDate endDate = LocalDate.of(2027, 12, 31);
		Lease l = lease().id(UUID.randomUUID()).status(LeaseStatus.ACTIVE).property(p).unit(u)
				.endDate(endDate).noticePeriodDays(60).build();
		Invite inv = makeInvite("x@test.com");
		LeaseTenant slot = makeSlot(l, t, inv, LeaseTenantRole.PRIMARY, null);

		when(organizationRepository.findAllByUserId(user.getId())).thenReturn(List.of(org));
		when(tenantRepository.findByUser_Id(user.getId())).thenReturn(Optional.of(t));
		when(leaseTenantRepository.findByTenantIdAndStatusInAndOrgId(any(), any(), any())).thenReturn(List.of(slot));
		when(assetRepository.findByUnit_Id(any())).thenReturn(List.of());
		when(leaseTenantRepository.findByLeaseIdWithTenantAndInvite(any())).thenReturn(List.of(slot));

		var result = service.getMyLease(user.getId(), org.getId());

		assertThat(result.lease().noticeDeadlineDate()).isEqualTo(endDate.minusDays(60));
	}

	@Test
	void shouldReturnNullNoticeDeadlineWhenNoticePeriodIsNull() {
		Organization org = makeOrg();
		User user = user().build();
		Tenant t = tenant().user(user).build();
		Prop p = makeProp(org);
		Unit u = makeUnit(p);
		Lease l = lease().id(UUID.randomUUID()).status(LeaseStatus.ACTIVE).property(p).unit(u)
				.noticePeriodDays(null).build();
		Invite inv = makeInvite("x@test.com");
		LeaseTenant slot = makeSlot(l, t, inv, LeaseTenantRole.PRIMARY, null);

		when(organizationRepository.findAllByUserId(user.getId())).thenReturn(List.of(org));
		when(tenantRepository.findByUser_Id(user.getId())).thenReturn(Optional.of(t));
		when(leaseTenantRepository.findByTenantIdAndStatusInAndOrgId(any(), any(), any())).thenReturn(List.of(slot));
		when(assetRepository.findByUnit_Id(any())).thenReturn(List.of());
		when(leaseTenantRepository.findByLeaseIdWithTenantAndInvite(any())).thenReturn(List.of(slot));

		var result = service.getMyLease(user.getId(), org.getId());

		assertThat(result.lease().noticeDeadlineDate()).isNull();
	}

	@Test
	void shouldPopulateAssetsCorrectly() {
		Organization org = makeOrg();
		User user = user().build();
		Tenant t = tenant().user(user).build();
		Prop p = makeProp(org);
		Unit u = makeUnit(p);
		Lease l = lease().id(UUID.randomUUID()).status(LeaseStatus.ACTIVE).property(p).unit(u).build();
		Invite inv = makeInvite("x@test.com");
		LeaseTenant slot = makeSlot(l, t, inv, LeaseTenantRole.PRIMARY, null);
		Asset asset = Asset.builder()
				.id(UUID.randomUUID())
				.category(AssetCategory.APPLIANCE)
				.makeModel("Whirlpool WDT750SAHZ")
				.build();

		when(organizationRepository.findAllByUserId(user.getId())).thenReturn(List.of(org));
		when(tenantRepository.findByUser_Id(user.getId())).thenReturn(Optional.of(t));
		when(leaseTenantRepository.findByTenantIdAndStatusInAndOrgId(any(), any(), any())).thenReturn(List.of(slot));
		when(assetRepository.findByUnit_Id(u.getId())).thenReturn(List.of(asset));
		when(leaseTenantRepository.findByLeaseIdWithTenantAndInvite(any())).thenReturn(List.of(slot));

		var result = service.getMyLease(user.getId(), org.getId());

		assertThat(result.assets()).hasSize(1);
		assertThat(result.assets().get(0).category()).isEqualTo(AssetCategory.APPLIANCE);
		assertThat(result.assets().get(0).makeModel()).isEqualTo("Whirlpool WDT750SAHZ");
	}

	@Test
	void shouldDeriveCoTenantStatusCorrectly() {
		Organization org = makeOrg();
		User primaryUser = user().name("Alice").build();
		Tenant primaryTenant = tenant().user(primaryUser).build();
		Prop p = makeProp(org);
		Unit u = makeUnit(p);
		Lease l = lease().id(UUID.randomUUID()).status(LeaseStatus.ACTIVE).property(p).unit(u).build();
		Invite primaryInv = makeInvite("alice@test.com");
		LeaseTenant primarySlot = makeSlot(l, primaryTenant, primaryInv, LeaseTenantRole.PRIMARY,
				LocalDate.now().minusDays(1));

		// Co-tenant who signed
		User coUser = user().name("Bob").build();
		Tenant coTenant = tenant().user(coUser).build();
		Invite coInv = makeInvite("bob@test.com");
		LeaseTenant signedSlot = makeSlot(l, coTenant, coInv, LeaseTenantRole.OCCUPANT,
				LocalDate.now().minusDays(1));

		// Co-tenant registered but not signed
		User regUser = user().name("Carol").build();
		Tenant regTenant = tenant().user(regUser).build();
		Invite regInv = makeInvite("carol@test.com");
		LeaseTenant registeredSlot = makeSlot(l, regTenant, regInv, LeaseTenantRole.OCCUPANT, null);

		// Invited but not yet registered
		Invite invitedInv = makeInvite("dave@test.com");
		LeaseTenant invitedSlot = makeSlot(l, null, invitedInv, LeaseTenantRole.OCCUPANT, null);

		when(organizationRepository.findAllByUserId(primaryUser.getId())).thenReturn(List.of(org));
		when(tenantRepository.findByUser_Id(primaryUser.getId())).thenReturn(Optional.of(primaryTenant));
		when(leaseTenantRepository.findByTenantIdAndStatusInAndOrgId(any(), any(), any()))
				.thenReturn(List.of(primarySlot));
		when(assetRepository.findByUnit_Id(any())).thenReturn(List.of());
		when(leaseTenantRepository.findByLeaseIdWithTenantAndInvite(any()))
				.thenReturn(List.of(primarySlot, signedSlot, registeredSlot, invitedSlot));

		var result = service.getMyLease(primaryUser.getId(), org.getId());

		assertThat(result.coTenants()).hasSize(3);

		var signed = result.coTenants().stream()
				.filter(c -> c.name().equals("Bob")).findFirst().orElseThrow();
		assertThat(signed.status()).isEqualTo(LeaseTenantStatus.SIGNED);

		var registered = result.coTenants().stream()
				.filter(c -> c.name().equals("Carol")).findFirst().orElseThrow();
		assertThat(registered.status()).isEqualTo(LeaseTenantStatus.REGISTERED);

		var invited = result.coTenants().stream()
				.filter(c -> c.name().equals("dave@test.com")).findFirst().orElseThrow();
		assertThat(invited.status()).isEqualTo(LeaseTenantStatus.INVITED);
	}

	@Test
	void shouldFallbackToInviteEmailForUnregisteredCoTenant() {
		Organization org = makeOrg();
		User user = user().build();
		Tenant t = tenant().user(user).build();
		Prop p = makeProp(org);
		Unit u = makeUnit(p);
		Lease l = lease().id(UUID.randomUUID()).status(LeaseStatus.ACTIVE).property(p).unit(u).build();
		Invite myInv = makeInvite("me@test.com");
		Invite unregInv = makeInvite("unreg@test.com");
		LeaseTenant mySlot = makeSlot(l, t, myInv, LeaseTenantRole.PRIMARY, null);
		LeaseTenant unregSlot = makeSlot(l, null, unregInv, LeaseTenantRole.OCCUPANT, null);

		when(organizationRepository.findAllByUserId(user.getId())).thenReturn(List.of(org));
		when(tenantRepository.findByUser_Id(user.getId())).thenReturn(Optional.of(t));
		when(leaseTenantRepository.findByTenantIdAndStatusInAndOrgId(any(), any(), any())).thenReturn(List.of(mySlot));
		when(assetRepository.findByUnit_Id(any())).thenReturn(List.of());
		when(leaseTenantRepository.findByLeaseIdWithTenantAndInvite(any()))
				.thenReturn(List.of(mySlot, unregSlot));

		var result = service.getMyLease(user.getId(), org.getId());

		assertThat(result.coTenants()).hasSize(1);
		assertThat(result.coTenants().get(0).name()).isEqualTo("unreg@test.com");
	}
}
