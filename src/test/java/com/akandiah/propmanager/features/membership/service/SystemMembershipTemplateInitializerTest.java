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

import com.akandiah.propmanager.features.membership.domain.MembershipTemplate;
import com.akandiah.propmanager.features.membership.domain.MembershipTemplateRepository;

@ExtendWith(MockitoExtension.class)
class SystemMembershipTemplateInitializerTest {

	@Mock
	private MembershipTemplateRepository templateRepository;

	@InjectMocks
	private SystemMembershipTemplateInitializer initializer;

	private static final DefaultApplicationArguments NO_ARGS =
			new DefaultApplicationArguments(new String[0]);

	@Test
	void seedsAllFourTemplates_whenNoneExist() throws Exception {
		when(templateRepository.existsById(any())).thenReturn(false);

		initializer.run(NO_ARGS);

		var captor = org.mockito.ArgumentCaptor.forClass(MembershipTemplate.class);
		verify(templateRepository, times(4)).save(captor.capture());
		captor.getAllValues().forEach(t -> {
			org.assertj.core.api.Assertions.assertThat(t.getVersion()).isEqualTo(0);
		});
	}

	@Test
	void seedsOnlyMissingTemplates_whenSomeAlreadyExist() throws Exception {
		when(templateRepository.existsById(SystemMembershipTemplateInitializer.PROPERTY_MANAGER_ID))
				.thenReturn(true);
		when(templateRepository.existsById(SystemMembershipTemplateInitializer.ORG_ADMIN_ID))
				.thenReturn(true);
		when(templateRepository.existsById(SystemMembershipTemplateInitializer.MAINTENANCE_ID))
				.thenReturn(false);
		when(templateRepository.existsById(SystemMembershipTemplateInitializer.VIEWER_ID))
				.thenReturn(false);

		initializer.run(NO_ARGS);

		verify(templateRepository, times(2)).save(any(MembershipTemplate.class));
	}

	@Test
	void skipsAllTemplates_whenAllAlreadyExist() throws Exception {
		when(templateRepository.existsById(any())).thenReturn(true);

		initializer.run(NO_ARGS);

		verify(templateRepository, never()).save(any(MembershipTemplate.class));
	}
}
