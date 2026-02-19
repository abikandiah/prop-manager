package com.akandiah.propmanager.features.tenant.service;

import static com.akandiah.propmanager.TestDataFactory.tenant;
import static com.akandiah.propmanager.TestDataFactory.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.features.tenant.api.dto.UpdateTenantRequest;
import com.akandiah.propmanager.features.tenant.domain.Tenant;
import com.akandiah.propmanager.features.tenant.domain.TenantRepository;
import com.akandiah.propmanager.features.user.domain.User;

import jakarta.persistence.OptimisticLockException;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

	@Mock
	private TenantRepository tenantRepository;

	@InjectMocks
	private TenantService service;

	// ───────────────────────── findByUser ─────────────────────────

	@Test
	void shouldReturnTenantProfileWhenExists() {
		User user = user().build();
		Tenant tenant = tenant().user(user).build();
		when(tenantRepository.findByUser_Id(user.getId())).thenReturn(Optional.of(tenant));

		var result = service.findByUser(user);

		assertThat(result).isPresent();
		assertThat(result.get().userId()).isEqualTo(user.getId());
		assertThat(result.get().emergencyContactName()).isEqualTo(tenant.getEmergencyContactName());
	}

	@Test
	void shouldReturnEmptyWhenNoTenantProfile() {
		User user = user().build();
		when(tenantRepository.findByUser_Id(user.getId())).thenReturn(Optional.empty());

		var result = service.findByUser(user);

		assertThat(result).isEmpty();
	}

	// ───────────────────────── updateByUser ─────────────────────────

	@Test
	void shouldUpdateTenantProfile() {
		User user = user().build();
		Tenant tenant = tenant().user(user).version(1).build();
		when(tenantRepository.findByUser_Id(user.getId())).thenReturn(Optional.of(tenant));
		when(tenantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		var req = new UpdateTenantRequest("Jane Emergency", "+1-416-555-0200", true, "1 cat", "Honda Civic", "Quiet renter", 1);
		var result = service.updateByUser(user, req);

		var captor = ArgumentCaptor.forClass(Tenant.class);
		verify(tenantRepository).save(captor.capture());
		assertThat(captor.getValue().getEmergencyContactName()).isEqualTo("Jane Emergency");
		assertThat(captor.getValue().getHasPets()).isTrue();
		assertThat(captor.getValue().getPetDescription()).isEqualTo("1 cat");
		assertThat(result.emergencyContactName()).isEqualTo("Jane Emergency");
	}

	@Test
	void shouldOnlyUpdateProvidedFields() {
		User user = user().build();
		Tenant tenant = tenant().user(user).emergencyContactName("Original").version(0).build();
		when(tenantRepository.findByUser_Id(user.getId())).thenReturn(Optional.of(tenant));
		when(tenantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		// Only updating notes — all other fields null (no-op)
		var req = new UpdateTenantRequest(null, null, null, null, null, "Updated note", 0);
		service.updateByUser(user, req);

		var captor = ArgumentCaptor.forClass(Tenant.class);
		verify(tenantRepository).save(captor.capture());
		assertThat(captor.getValue().getEmergencyContactName()).isEqualTo("Original");
		assertThat(captor.getValue().getNotes()).isEqualTo("Updated note");
	}

	@Test
	void shouldThrowWhenUpdatingNonExistentProfile() {
		User user = user().build();
		when(tenantRepository.findByUser_Id(user.getId())).thenReturn(Optional.empty());

		var req = new UpdateTenantRequest(null, null, null, null, null, null, 0);

		assertThatThrownBy(() -> service.updateByUser(user, req))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void shouldThrowOnVersionMismatch() {
		User user = user().build();
		Tenant tenant = tenant().user(user).version(3).build();
		when(tenantRepository.findByUser_Id(user.getId())).thenReturn(Optional.of(tenant));

		var req = new UpdateTenantRequest(null, null, null, null, null, null, 1); // stale version

		assertThatThrownBy(() -> service.updateByUser(user, req))
				.isInstanceOf(OptimisticLockException.class);
	}
}
