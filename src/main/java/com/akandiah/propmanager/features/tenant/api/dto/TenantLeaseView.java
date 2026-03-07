package com.akandiah.propmanager.features.tenant.api.dto;

import java.util.List;
import java.util.UUID;

import com.akandiah.propmanager.features.lease.domain.LeaseTenantStatus;

/** Aggregate view of a tenant's current lease context. Returned by GET /api/tenant/my-lease. */
public record TenantLeaseView(
		TenantLeaseSummary lease,
		TenantPropertySummary property,
		TenantUnitSummary unit,
		List<TenantAssetSummary> assets,
		List<TenantCoTenantSummary> coTenants,
		/** The calling tenant's own LeaseTenant ID — used to invoke the sign endpoint. */
		UUID myLeaseTenantId,
		/** The calling tenant's current signing status on this lease. */
		LeaseTenantStatus myLeaseTenantStatus) {
}
