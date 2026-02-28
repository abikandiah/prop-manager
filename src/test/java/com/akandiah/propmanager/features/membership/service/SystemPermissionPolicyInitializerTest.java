package com.akandiah.propmanager.features.membership.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import com.akandiah.propmanager.features.membership.domain.PermissionPolicy;
import com.akandiah.propmanager.features.membership.domain.PermissionPolicyRepository;

@ExtendWith(MockitoExtension.class)
class SystemPermissionPolicyInitializerTest {

	@Mock
	private PermissionPolicyRepository policyRepository;

	@InjectMocks
	private SystemPermissionPolicyInitializer initializer;

	private static final DefaultApplicationArguments NO_ARGS =
			new DefaultApplicationArguments(new String[0]);

	@Test
	void seedsAllFourPolicies_whenNoneExist() throws Exception {
		when(policyRepository.existsById(any())).thenReturn(false);

		initializer.run(NO_ARGS);

		var captor = org.mockito.ArgumentCaptor.forClass(PermissionPolicy.class);
		verify(policyRepository, times(4)).save(captor.capture());
		captor.getAllValues().forEach(p -> {
			org.assertj.core.api.Assertions.assertThat(p.getId()).isNotNull();
			org.assertj.core.api.Assertions.assertThat(p.getName()).isNotBlank();
			org.assertj.core.api.Assertions.assertThat(p.getPermissions()).isNotEmpty();
		});
	}

	@Test
	void seedsOnlyMissingPolicies_whenSomeAlreadyExist() throws Exception {
		when(policyRepository.existsById(SystemPermissionPolicyInitializer.PROPERTY_MANAGER_ID))
				.thenReturn(true);
		when(policyRepository.existsById(SystemPermissionPolicyInitializer.ORG_ADMIN_ID))
				.thenReturn(true);
		when(policyRepository.existsById(SystemPermissionPolicyInitializer.MAINTENANCE_ID))
				.thenReturn(false);
		when(policyRepository.existsById(SystemPermissionPolicyInitializer.VIEWER_ID))
				.thenReturn(false);

		initializer.run(NO_ARGS);

		verify(policyRepository, times(2)).save(any(PermissionPolicy.class));
	}

	@Test
	void skipsAllPolicies_whenAllAlreadyExist() throws Exception {
		when(policyRepository.existsById(any())).thenReturn(true);

		initializer.run(NO_ARGS);

		verify(policyRepository, never()).save(any(PermissionPolicy.class));
	}
}
