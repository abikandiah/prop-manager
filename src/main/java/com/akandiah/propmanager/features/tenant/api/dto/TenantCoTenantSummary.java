package com.akandiah.propmanager.features.tenant.api.dto;

import com.akandiah.propmanager.features.lease.domain.LeaseTenantRole;
import com.akandiah.propmanager.features.lease.domain.LeaseTenantStatus;

/** Minimal co-tenant info — name and role only. No contact details (privacy). */
public record TenantCoTenantSummary(
		String name,
		LeaseTenantRole role,
		LeaseTenantStatus status) {
}
