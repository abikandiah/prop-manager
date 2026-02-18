package com.akandiah.propmanager.features.tenant.api;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.akandiah.propmanager.features.tenant.api.dto.TenantResponse;
import com.akandiah.propmanager.features.tenant.service.TenantService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/tenants")
@Tag(name = "Tenants", description = "Tenant read operations")
public class TenantController {

	private final TenantService tenantService;

	public TenantController(TenantService tenantService) {
		this.tenantService = tenantService;
	}

	@GetMapping
	@Operation(summary = "List all tenants")
	public List<TenantResponse> list() {
		return tenantService.findAll();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get tenant by ID")
	public TenantResponse getById(@PathVariable UUID id) {
		return tenantService.findById(id);
	}
}
