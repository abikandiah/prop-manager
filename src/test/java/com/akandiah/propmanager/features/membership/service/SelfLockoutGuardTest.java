package com.akandiah.propmanager.features.membership.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.akandiah.propmanager.common.exception.SelfLockoutException;
import com.akandiah.propmanager.common.permission.ResourceType;
import com.akandiah.propmanager.features.membership.domain.Membership;
import com.akandiah.propmanager.features.membership.domain.MembershipRepository;
import com.akandiah.propmanager.features.membership.domain.PermissionPolicy;
import com.akandiah.propmanager.features.membership.domain.PolicyAssignment;
import com.akandiah.propmanager.features.membership.domain.PolicyAssignmentRepository;
import com.akandiah.propmanager.features.organization.domain.Organization;
import com.akandiah.propmanager.features.user.domain.User;
import com.akandiah.propmanager.security.JwtUserResolver;

@ExtendWith(MockitoExtension.class)
class SelfLockoutGuardTest {

	@Mock private PolicyAssignmentRepository assignmentRepository;
	@Mock private MembershipRepository membershipRepository;
	@Mock private JwtUserResolver jwtUserResolver;

	@InjectMocks
	private SelfLockoutGuard guard;

	@BeforeEach
	void setUserContext() {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken("user", null,
						List.of(new SimpleGrantedAuthority("ROLE_USER"))));
	}

	@AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
	}

	private void setAdminContext() {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken("admin", null,
						List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
	}

	// ─── requireNoSelfLockout ─────────────────────────────────────────────────

	@Test
	void requireNoSelfLockout_skipsNonOrgAssignment() {
		Membership m = membership(UUID.randomUUID(), UUID.randomUUID(), user(UUID.randomUUID()));
		PolicyAssignment assignment = PolicyAssignment.builder()
				.id(UUID.randomUUID()).membership(m)
				.resourceType(ResourceType.PROPERTY).resourceId(UUID.randomUUID())
				.build();

		assertThatCode(() -> guard.requireNoSelfLockout(assignment, null, null))
				.doesNotThrowAnyException();

		verify(jwtUserResolver, never()).resolveOptionalId();
	}

	@Test
	void requireNoSelfLockout_skipsForGlobalAdmin() {
		setAdminContext();
		UUID userId = UUID.randomUUID();
		UUID membershipId = UUID.randomUUID();
		Membership m = membership(membershipId, UUID.randomUUID(), user(userId));
		PolicyAssignment assignment = orgAssignment(UUID.randomUUID(), m, Map.of("o", "r"));

		assertThatCode(() -> guard.requireNoSelfLockout(assignment, null, null))
				.doesNotThrowAnyException();

		verify(jwtUserResolver, never()).resolveOptionalId();
	}

	@Test
	void requireNoSelfLockout_skipsWhenDifferentUser() {
		UUID membershipUserId = UUID.randomUUID();
		UUID currentUserId = UUID.randomUUID(); // different
		UUID membershipId = UUID.randomUUID();
		Membership m = membership(membershipId, UUID.randomUUID(), user(membershipUserId));
		PolicyAssignment assignment = orgAssignment(UUID.randomUUID(), m, Map.of("o", "r"));

		when(jwtUserResolver.resolveOptionalId()).thenReturn(Optional.of(currentUserId));

		assertThatCode(() -> guard.requireNoSelfLockout(assignment, null, null))
				.doesNotThrowAnyException();
	}

	@Test
	void requireNoSelfLockout_allowsWhenOtherAssignmentGrantsOrgRead() {
		UUID userId = UUID.randomUUID();
		UUID membershipId = UUID.randomUUID();
		Membership m = membership(membershipId, UUID.randomUUID(), user(userId));
		UUID orgId = m.getOrganization().getId();
		PolicyAssignment target = orgAssignment(UUID.randomUUID(), m, Map.of("o", "r"));
		PolicyAssignment other = orgAssignment(UUID.randomUUID(), m, Map.of("o", "r"));

		when(jwtUserResolver.resolveOptionalId()).thenReturn(Optional.of(userId));
		when(assignmentRepository.findByMembershipIdInAndResourceTypeWithPolicy(
				List.of(membershipId), ResourceType.ORG))
				.thenReturn(List.of(target, other));

		// delete target (null new state) — other still grants access
		assertThatCode(() -> guard.requireNoSelfLockout(target, null, null))
				.doesNotThrowAnyException();
	}

	@Test
	void requireNoSelfLockout_allowsWhenNewStateGrantsOrgRead() {
		UUID userId = UUID.randomUUID();
		UUID membershipId = UUID.randomUUID();
		Membership m = membership(membershipId, UUID.randomUUID(), user(userId));
		PolicyAssignment target = orgAssignment(UUID.randomUUID(), m, null);

		when(jwtUserResolver.resolveOptionalId()).thenReturn(Optional.of(userId));
		when(assignmentRepository.findByMembershipIdInAndResourceTypeWithPolicy(
				List.of(membershipId), ResourceType.ORG))
				.thenReturn(List.of(target)); // only one

		// Update gives it a policy with o:r — still has access after change
		PermissionPolicy newPolicy = PermissionPolicy.builder()
				.id(UUID.randomUUID()).permissions(Map.of("o", "r")).build();

		assertThatCode(() -> guard.requireNoSelfLockout(target, null, newPolicy))
				.doesNotThrowAnyException();
	}

	@Test
	void requireNoSelfLockout_throwsOnDeleteOfLastOrgReadAssignment() {
		UUID userId = UUID.randomUUID();
		UUID membershipId = UUID.randomUUID();
		Membership m = membership(membershipId, UUID.randomUUID(), user(userId));
		PolicyAssignment target = orgAssignment(UUID.randomUUID(), m, Map.of("o", "r"));

		when(jwtUserResolver.resolveOptionalId()).thenReturn(Optional.of(userId));
		when(assignmentRepository.findByMembershipIdInAndResourceTypeWithPolicy(
				List.of(membershipId), ResourceType.ORG))
				.thenReturn(List.of(target)); // only one

		assertThatThrownBy(() -> guard.requireNoSelfLockout(target, null, null))
				.isInstanceOf(SelfLockoutException.class);
	}

	@Test
	void requireNoSelfLockout_throwsWhenUpdateRemovesLastOrgRead() {
		UUID userId = UUID.randomUUID();
		UUID membershipId = UUID.randomUUID();
		Membership m = membership(membershipId, UUID.randomUUID(), user(userId));
		PolicyAssignment target = orgAssignment(UUID.randomUUID(), m, Map.of("o", "r"));

		when(jwtUserResolver.resolveOptionalId()).thenReturn(Optional.of(userId));
		when(assignmentRepository.findByMembershipIdInAndResourceTypeWithPolicy(
				List.of(membershipId), ResourceType.ORG))
				.thenReturn(List.of(target));

		// Update clears org permissions — loses READ
		assertThatThrownBy(() -> guard.requireNoSelfLockout(target, Map.of(), null))
				.isInstanceOf(SelfLockoutException.class);
	}

	// ─── requireNoSelfLockoutOnMembershipDeletion ────────────────────────────

	@Test
	void onMembershipDeletion_skipsForGlobalAdmin() {
		setAdminContext();
		Membership m = membership(UUID.randomUUID(), UUID.randomUUID(), user(UUID.randomUUID()));

		assertThatCode(() -> guard.requireNoSelfLockoutOnMembershipDeletion(m))
				.doesNotThrowAnyException();

		verify(jwtUserResolver, never()).resolveOptionalId();
	}

	@Test
	void onMembershipDeletion_throwsWhenDeletingOwnMembership() {
		UUID userId = UUID.randomUUID();
		Membership m = membership(UUID.randomUUID(), UUID.randomUUID(), user(userId));

		when(jwtUserResolver.resolveOptionalId()).thenReturn(Optional.of(userId));

		assertThatThrownBy(() -> guard.requireNoSelfLockoutOnMembershipDeletion(m))
				.isInstanceOf(SelfLockoutException.class);
	}

	@Test
	void onMembershipDeletion_skipsOrphanCheckWhenMembershipHasNoOrgRead() {
		UUID currentUserId = UUID.randomUUID();
		UUID membershipUserId = UUID.randomUUID(); // different from current
		Membership m = membership(UUID.randomUUID(), UUID.randomUUID(), user(membershipUserId));

		when(jwtUserResolver.resolveOptionalId()).thenReturn(Optional.of(currentUserId));
		when(assignmentRepository.findByMembershipIdInAndResourceTypeWithPolicy(
				List.of(m.getId()), ResourceType.ORG))
				.thenReturn(List.of()); // no ORG-scoped assignments

		assertThatCode(() -> guard.requireNoSelfLockoutOnMembershipDeletion(m))
				.doesNotThrowAnyException();

		// membershipRepository.findByOrganizationId never needed since no o:r to protect
		verify(membershipRepository, never()).findByOrganizationId(any());
	}

	@Test
	void onMembershipDeletion_throwsWhenOrphanedOrg() {
		UUID currentUserId = UUID.randomUUID();
		UUID membershipUserId = UUID.randomUUID();
		UUID orgId = UUID.randomUUID();
		Membership target = membership(UUID.randomUUID(), orgId, user(membershipUserId));
		PolicyAssignment orgReadAssignment = orgAssignment(UUID.randomUUID(), target, Map.of("o", "r"));

		when(jwtUserResolver.resolveOptionalId()).thenReturn(Optional.of(currentUserId));
		when(assignmentRepository.findByMembershipIdInAndResourceTypeWithPolicy(
				List.of(target.getId()), ResourceType.ORG))
				.thenReturn(List.of(orgReadAssignment));
		// Only the target membership exists in the org
		when(membershipRepository.findByOrganizationId(orgId)).thenReturn(List.of(target));

		assertThatThrownBy(() -> guard.requireNoSelfLockoutOnMembershipDeletion(target))
				.isInstanceOf(SelfLockoutException.class)
				.hasMessageContaining("administrators");
	}

	@Test
	void onMembershipDeletion_allowsWhenOtherMembershipHasOrgRead() {
		UUID currentUserId = UUID.randomUUID();
		UUID membershipUserId = UUID.randomUUID();
		UUID orgId = UUID.randomUUID();
		Membership target = membership(UUID.randomUUID(), orgId, user(membershipUserId));
		Membership other = membership(UUID.randomUUID(), orgId, user(UUID.randomUUID()));
		PolicyAssignment targetOrgRead = orgAssignment(UUID.randomUUID(), target, Map.of("o", "r"));
		PolicyAssignment otherOrgRead = orgAssignment(UUID.randomUUID(), other, Map.of("o", "r"));

		when(jwtUserResolver.resolveOptionalId()).thenReturn(Optional.of(currentUserId));
		when(assignmentRepository.findByMembershipIdInAndResourceTypeWithPolicy(
				List.of(target.getId()), ResourceType.ORG))
				.thenReturn(List.of(targetOrgRead));
		when(membershipRepository.findByOrganizationId(orgId)).thenReturn(List.of(target, other));
		when(assignmentRepository.findByMembershipIdInAndResourceTypeWithPolicy(
				List.of(other.getId()), ResourceType.ORG))
				.thenReturn(List.of(otherOrgRead));

		assertThatCode(() -> guard.requireNoSelfLockoutOnMembershipDeletion(target))
				.doesNotThrowAnyException();
	}

	@Test
	void onMembershipDeletion_skipsWhenNullUser() {
		UUID currentUserId = UUID.randomUUID();
		// Pending invite membership — user is null
		Membership m = Membership.builder()
				.id(UUID.randomUUID())
				.user(null)
				.organization(Organization.builder().id(UUID.randomUUID()).build())
				.build();

		when(jwtUserResolver.resolveOptionalId()).thenReturn(Optional.of(currentUserId));
		when(assignmentRepository.findByMembershipIdInAndResourceTypeWithPolicy(
				List.of(m.getId()), ResourceType.ORG))
				.thenReturn(List.of()); // no o:r

		assertThatCode(() -> guard.requireNoSelfLockoutOnMembershipDeletion(m))
				.doesNotThrowAnyException();
	}

	// ─── requireNoSelfLockoutOnPolicyUpdate ─────────────────────────────────

	@Test
	void onPolicyUpdate_skipsForGlobalAdmin() {
		setAdminContext();
		guard.requireNoSelfLockoutOnPolicyUpdate(UUID.randomUUID(), Map.of(), UUID.randomUUID());

		verify(jwtUserResolver, never()).resolveOptionalId();
	}

	@Test
	void onPolicyUpdate_skipsWhenNullOrgId() {
		guard.requireNoSelfLockoutOnPolicyUpdate(UUID.randomUUID(), Map.of(), null);

		verify(jwtUserResolver, never()).resolveOptionalId();
	}

	@Test
	void onPolicyUpdate_skipsWhenUserNotInOrg() {
		UUID currentUserId = UUID.randomUUID();
		UUID orgId = UUID.randomUUID();

		when(jwtUserResolver.resolveOptionalId()).thenReturn(Optional.of(currentUserId));
		when(membershipRepository.findByUserIdAndOrganizationId(currentUserId, orgId))
				.thenReturn(Optional.empty());

		assertThatCode(() -> guard.requireNoSelfLockoutOnPolicyUpdate(UUID.randomUUID(), Map.of(), orgId))
				.doesNotThrowAnyException();
	}

	@Test
	void onPolicyUpdate_throwsWhenUpdateRemovesLastOrgRead() {
		UUID currentUserId = UUID.randomUUID();
		UUID orgId = UUID.randomUUID();
		UUID policyId = UUID.randomUUID();
		UUID membershipId = UUID.randomUUID();
		Membership m = membership(membershipId, orgId, user(currentUserId));

		PermissionPolicy policy = PermissionPolicy.builder()
				.id(policyId).permissions(Map.of("o", "r")).build();
		PolicyAssignment assignment = orgAssignment(UUID.randomUUID(), m, null);
		assignment.setPolicy(policy);

		when(jwtUserResolver.resolveOptionalId()).thenReturn(Optional.of(currentUserId));
		when(membershipRepository.findByUserIdAndOrganizationId(currentUserId, orgId))
				.thenReturn(Optional.of(m));
		when(assignmentRepository.findByMembershipIdInAndResourceTypeWithPolicy(
				List.of(membershipId), ResourceType.ORG))
				.thenReturn(List.of(assignment));

		// New permissions strip org READ
		assertThatThrownBy(() -> guard.requireNoSelfLockoutOnPolicyUpdate(policyId, Map.of(), orgId))
				.isInstanceOf(SelfLockoutException.class);
	}

	@Test
	void onPolicyUpdate_allowsWhenOverrideStillGrantsOrgRead() {
		UUID currentUserId = UUID.randomUUID();
		UUID orgId = UUID.randomUUID();
		UUID policyId = UUID.randomUUID();
		UUID membershipId = UUID.randomUUID();
		Membership m = membership(membershipId, orgId, user(currentUserId));

		PermissionPolicy policy = PermissionPolicy.builder()
				.id(policyId).permissions(Map.of("o", "r")).build();
		// Override explicitly grants o:r — takes precedence over policy
		PolicyAssignment assignment = orgAssignment(UUID.randomUUID(), m, Map.of("o", "r"));
		assignment.setPolicy(policy);

		when(jwtUserResolver.resolveOptionalId()).thenReturn(Optional.of(currentUserId));
		when(membershipRepository.findByUserIdAndOrganizationId(currentUserId, orgId))
				.thenReturn(Optional.of(m));
		when(assignmentRepository.findByMembershipIdInAndResourceTypeWithPolicy(
				List.of(membershipId), ResourceType.ORG))
				.thenReturn(List.of(assignment));

		// Policy stripped of o:r — but override still has it
		assertThatCode(() -> guard.requireNoSelfLockoutOnPolicyUpdate(policyId, Map.of(), orgId))
				.doesNotThrowAnyException();
	}

	@Test
	void onPolicyUpdate_allowsWhenOtherAssignmentGrantsOrgRead() {
		UUID currentUserId = UUID.randomUUID();
		UUID orgId = UUID.randomUUID();
		UUID policyId = UUID.randomUUID();
		UUID membershipId = UUID.randomUUID();
		Membership m = membership(membershipId, orgId, user(currentUserId));

		PermissionPolicy updatedPolicy = PermissionPolicy.builder()
				.id(policyId).permissions(Map.of("o", "r")).build();
		// First assignment uses the policy being updated (will lose o:r)
		PolicyAssignment affected = orgAssignment(UUID.randomUUID(), m, null);
		affected.setPolicy(updatedPolicy);
		// Second assignment has its own override with o:r — unaffected by the policy change
		PolicyAssignment unaffected = orgAssignment(UUID.randomUUID(), m, Map.of("o", "r"));

		when(jwtUserResolver.resolveOptionalId()).thenReturn(Optional.of(currentUserId));
		when(membershipRepository.findByUserIdAndOrganizationId(currentUserId, orgId))
				.thenReturn(Optional.of(m));
		when(assignmentRepository.findByMembershipIdInAndResourceTypeWithPolicy(
				List.of(membershipId), ResourceType.ORG))
				.thenReturn(List.of(affected, unaffected));

		assertThatCode(() -> guard.requireNoSelfLockoutOnPolicyUpdate(policyId, Map.of(), orgId))
				.doesNotThrowAnyException();
	}

	// ─── helpers ────────────────────────────────────────────────────────────

	private static User user(UUID id) {
		return User.builder().id(id).name("Test User").email("test@example.com").build();
	}

	private static Membership membership(UUID id, UUID orgId, User user) {
		Organization org = Organization.builder().id(orgId).name("Test Org").build();
		return Membership.builder().id(id).user(user).organization(org).build();
	}

	private static PolicyAssignment orgAssignment(UUID id, Membership membership,
			Map<String, String> overrides) {
		return PolicyAssignment.builder()
				.id(id)
				.membership(membership)
				.resourceType(ResourceType.ORG)
				.resourceId(membership.getOrganization().getId())
				.overrides(overrides)
				.build();
	}
}
