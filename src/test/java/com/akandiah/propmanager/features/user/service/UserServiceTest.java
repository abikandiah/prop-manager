package com.akandiah.propmanager.features.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.akandiah.propmanager.common.permission.AccessEntry;
import com.akandiah.propmanager.common.permission.Actions;
import com.akandiah.propmanager.common.permission.PermissionDomains;
import com.akandiah.propmanager.common.permission.ResourceType;
import com.akandiah.propmanager.features.user.api.dto.UiRole;
import com.akandiah.propmanager.features.user.domain.UserIdentityRepository;
import com.akandiah.propmanager.features.user.domain.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock UserRepository userRepository;
	@Mock UserIdentityRepository userIdentityRepository;
	@Mock ApplicationEventPublisher eventPublisher;

	@InjectMocks UserService userService;

	private static final UUID ORG_ID = UUID.randomUUID();

	// --- helpers ---

	private AccessEntry orgEntry(Map<String, Integer> permissions) {
		return new AccessEntry(ORG_ID, ResourceType.ORG, ORG_ID, permissions);
	}

	private AccessEntry propertyEntry(Map<String, Integer> permissions) {
		return new AccessEntry(ORG_ID, ResourceType.PROPERTY, UUID.randomUUID(), permissions);
	}

	private AccessEntry unitEntry(Map<String, Integer> permissions) {
		return new AccessEntry(ORG_ID, ResourceType.UNIT, UUID.randomUUID(), permissions);
	}

	// --- tests ---

	@Test
	void shouldDeriveAdminRoleForSystemAdmin() {
		List<UiRole> roles = userService.deriveRoles(List.of("ROLE_ADMIN"), List.of());

		assertThat(roles).containsExactly(UiRole.ADMIN);
	}

	@Test
	void shouldDeriveAdminRoleForOrgAdmin() {
		// Org admin: UPDATE on ORGANIZATION domain at ORG scope
		AccessEntry entry = orgEntry(Map.of(PermissionDomains.ORGANIZATION, Actions.READ | Actions.UPDATE));

		List<UiRole> roles = userService.deriveRoles(List.of(), List.of(entry));

		assertThat(roles).contains(UiRole.ADMIN);
	}

	@Test
	void shouldDeriveManagerRoleForPropertyOwner() {
		// Property owner lands at PROPERTY scope with PORTFOLIO READ — MANAGER, not ADMIN
		AccessEntry entry = propertyEntry(Map.of(
				PermissionDomains.PORTFOLIO, Actions.READ | Actions.CREATE | Actions.UPDATE | Actions.DELETE));

		List<UiRole> roles = userService.deriveRoles(List.of(), List.of(entry));

		assertThat(roles).containsExactly(UiRole.MANAGER);
		assertThat(roles).doesNotContain(UiRole.ADMIN);
	}

	@Test
	void shouldDeriveTenantRole() {
		AccessEntry entry = unitEntry(Map.of(PermissionDomains.LEASES, Actions.READ));

		List<UiRole> roles = userService.deriveRoles(List.of(), List.of(entry));

		assertThat(roles).containsExactly(UiRole.TENANT);
	}

	@Test
	void shouldDeriveOtherWhenNoMatch() {
		List<UiRole> roles = userService.deriveRoles(List.of(), List.of());

		assertThat(roles).containsExactly(UiRole.OTHER);
	}

	@Test
	void shouldDeriveBothManagerAndTenant() {
		AccessEntry managerEntry = orgEntry(Map.of(PermissionDomains.PORTFOLIO, Actions.READ));
		AccessEntry tenantEntry = unitEntry(Map.of(PermissionDomains.LEASES, Actions.READ));

		List<UiRole> roles = userService.deriveRoles(List.of(), List.of(managerEntry, tenantEntry));

		assertThat(roles).containsExactlyInAnyOrder(UiRole.MANAGER, UiRole.TENANT);
	}

	@Test
	void shouldDeriveOrgAdminAsAdminNotManager() {
		// ORGANIZATION UPDATE at ORG scope → ADMIN, but no PORTFOLIO READ → not MANAGER
		AccessEntry entry = orgEntry(Map.of(PermissionDomains.ORGANIZATION, Actions.UPDATE));

		List<UiRole> roles = userService.deriveRoles(List.of(), List.of(entry));

		assertThat(roles).containsExactly(UiRole.ADMIN);
		assertThat(roles).doesNotContain(UiRole.MANAGER);
	}
}
