package com.akandiah.propmanager.features.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import com.akandiah.propmanager.common.permission.AccessEntry;
import com.akandiah.propmanager.common.permission.Actions;
import com.akandiah.propmanager.common.permission.PermissionDomains;
import com.akandiah.propmanager.common.permission.ResourceType;
import com.akandiah.propmanager.features.user.api.dto.UiRole;
import com.akandiah.propmanager.features.user.domain.User;
import com.akandiah.propmanager.features.user.domain.UserIdentity;
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

	private static Jwt.Builder jwt() {
		return Jwt.withTokenValue("t").header("alg", "none").subject("sub1");
	}

	private static HttpStatus statusOf(Throwable t) {
		return HttpStatus.valueOf(((ResponseStatusException) t).getStatusCode().value());
	}

	// --- tests ---

	@Test
	void shouldReturnVerifiedEmail() {
		Jwt token = jwt().claim("email", "a@b.com").claim(UserService.EMAIL_VERIFIED_CLAIM, true).build();
		assertThat(UserService.requireVerifiedEmail(token)).isEqualTo("a@b.com");
	}

	@Test
	void shouldRejectUnverifiedEmail() {
		Jwt token = jwt().claim("email", "a@b.com").claim(UserService.EMAIL_VERIFIED_CLAIM, false).build();
		assertThatThrownBy(() -> UserService.requireVerifiedEmail(token))
				.satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.FORBIDDEN));
	}

	@Test
	void shouldRejectEmailWithoutVerifiedClaim() {
		Jwt token = jwt().claim("email", "a@b.com").build();
		assertThatThrownBy(() -> UserService.requireVerifiedEmail(token))
				.satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.FORBIDDEN));
	}

	@Test
	void shouldRejectMissingEmail() {
		Jwt token = jwt().claim(UserService.EMAIL_VERIFIED_CLAIM, true).build();
		assertThatThrownBy(() -> UserService.requireVerifiedEmail(token))
				.satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.BAD_REQUEST));
	}

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

	// --- getOrCreateUser: name no-overwrite ---

	@Test
	void shouldNotOverwriteExistingNameFromIdP() {
		User existingUser = User.builder().id(UUID.randomUUID()).name("Custom Name").email("a@b.com").build();
		UserIdentity identity = UserIdentity.builder().user(existingUser).issuer("iss").sub("sub1").build();
		when(userIdentityRepository.findByIssuerAndSubWithUser("iss", "sub1")).thenReturn(Optional.of(identity));
		when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		User result = userService.getOrCreateUser("iss", "sub1", "IdP Name", "a@b.com");

		assertThat(result.getName()).isEqualTo("Custom Name");
	}

	@Test
	void shouldSetNameFromIdPWhenDbNameIsBlank() {
		User existingUser = User.builder().id(UUID.randomUUID()).name("").email("a@b.com").build();
		UserIdentity identity = UserIdentity.builder().user(existingUser).issuer("iss").sub("sub1").build();
		when(userIdentityRepository.findByIssuerAndSubWithUser("iss", "sub1")).thenReturn(Optional.of(identity));
		when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		User result = userService.getOrCreateUser("iss", "sub1", "IdP Name", "a@b.com");

		assertThat(result.getName()).isEqualTo("IdP Name");
	}

	// --- getOrCreateUser: email conflict guard ---

	@Test
	void shouldThrowWhenEmailAlreadyOwnedByAnotherAccount() {
		UUID userId = UUID.randomUUID();
		UUID otherUserId = UUID.randomUUID();
		User existingUser = User.builder().id(userId).name("Alice").email("old@b.com").build();
		User otherUser = User.builder().id(otherUserId).name("Bob").email("new@b.com").build();
		UserIdentity identity = UserIdentity.builder().user(existingUser).issuer("iss").sub("sub1").build();
		when(userIdentityRepository.findByIssuerAndSubWithUser("iss", "sub1")).thenReturn(Optional.of(identity));
		when(userRepository.findByEmail("new@b.com")).thenReturn(Optional.of(otherUser));

		assertThatThrownBy(() -> userService.getOrCreateUser("iss", "sub1", "Alice", "new@b.com"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("already associated with another account");
	}

	// --- updateProfile ---

	@Test
	void shouldUpdateNameAndPhoneNumber() {
		User user = User.builder().id(UUID.randomUUID()).name("Old Name").email("a@b.com").build();
		when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		User result = userService.updateProfile(user, "New Name", "+1-555-0100");

		var captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(captor.capture());
		assertThat(captor.getValue().getName()).isEqualTo("New Name");
		assertThat(captor.getValue().getPhoneNumber()).isEqualTo("+1-555-0100");
		assertThat(result.getName()).isEqualTo("New Name");
	}

	@Test
	void shouldClearPhoneNumberWhenBlank() {
		User user = User.builder().id(UUID.randomUUID()).name("Alice").email("a@b.com").phoneNumber("+1-555-0100").build();
		when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		userService.updateProfile(user, null, "");

		var captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(captor.capture());
		assertThat(captor.getValue().getPhoneNumber()).isNull();
	}
}
