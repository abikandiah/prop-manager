package com.akandiah.propmanager.features.lease.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.akandiah.propmanager.features.lease.domain.LeaseTemplate;
import com.akandiah.propmanager.features.lease.domain.LeaseTemplateRepository;
import com.akandiah.propmanager.features.organization.domain.Organization;
import com.akandiah.propmanager.features.organization.domain.OrganizationCreatedEvent;
import com.akandiah.propmanager.features.organization.domain.OrganizationRepository;

@ExtendWith(MockitoExtension.class)
class DefaultLeaseTemplatesInitializerTest {

	@Mock
	private LeaseTemplateRepository repository;

	@Mock
	private OrganizationRepository organizationRepository;

	@Mock
	private ResourceLoader resourceLoader;

	@Mock
	private Resource resource;

	private DefaultLeaseTemplatesInitializer initializer;

	@BeforeEach
	void setUp() {
		initializer = new DefaultLeaseTemplatesInitializer(repository, organizationRepository, resourceLoader);
	}

	@Test
	void seedsTemplates_whenOrganizationCreatedAndHasNoTemplates() throws Exception {
		UUID orgId = UUID.randomUUID();
		Organization org = Organization.builder().id(orgId).name("Test Org").build();
		OrganizationCreatedEvent event = new OrganizationCreatedEvent(orgId);

		when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
		when(repository.countByOrg_Id(orgId)).thenReturn(0L);
		when(resourceLoader.getResource(anyString())).thenReturn(resource);
		when(resource.getContentAsString(StandardCharsets.UTF_8)).thenReturn("Mock Markdown");

		initializer.onOrganizationCreated(event);

		// Expect 2 templates to be seeded (Residential and Commercial)
		verify(repository, times(2)).save(any(LeaseTemplate.class));
	}

	@Test
	void skipsSeeding_whenOrganizationAlreadyHasTemplates() {
		UUID orgId = UUID.randomUUID();
		Organization org = Organization.builder().id(orgId).name("Test Org").build();
		OrganizationCreatedEvent event = new OrganizationCreatedEvent(orgId);

		when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
		when(repository.countByOrg_Id(orgId)).thenReturn(5L);

		initializer.onOrganizationCreated(event);

		verify(repository, never()).save(any(LeaseTemplate.class));
		verify(resourceLoader, never()).getResource(anyString());
	}
}
