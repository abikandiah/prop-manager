package com.akandiah.propmanager.features.tenant.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.features.tenant.api.dto.TenantResponse;
import com.akandiah.propmanager.features.tenant.domain.Tenant;
import com.akandiah.propmanager.features.tenant.domain.TenantRepository;

@Service
public class TenantService {

	private final TenantRepository tenantRepository;

	public TenantService(TenantRepository tenantRepository) {
		this.tenantRepository = tenantRepository;
	}

	@Transactional(readOnly = true)
	public List<TenantResponse> findAll() {
		return tenantRepository.findAll().stream()
				.map(TenantResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public TenantResponse findById(UUID id) {
		Tenant tenant = tenantRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Tenant", id));
		return TenantResponse.from(tenant);
	}
}
