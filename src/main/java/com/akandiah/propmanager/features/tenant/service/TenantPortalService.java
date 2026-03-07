package com.akandiah.propmanager.features.tenant.service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.features.asset.domain.Asset;
import com.akandiah.propmanager.features.asset.domain.AssetRepository;
import com.akandiah.propmanager.features.lease.domain.Lease;
import com.akandiah.propmanager.features.lease.domain.LeaseStatus;
import com.akandiah.propmanager.features.lease.domain.LeaseTenant;
import com.akandiah.propmanager.features.lease.domain.LeaseTenantRepository;
import com.akandiah.propmanager.features.organization.domain.Organization;
import com.akandiah.propmanager.features.organization.domain.OrganizationRepository;
import com.akandiah.propmanager.features.tenant.api.dto.TenantAssetSummary;
import com.akandiah.propmanager.features.tenant.api.dto.TenantCoTenantSummary;
import com.akandiah.propmanager.features.tenant.api.dto.TenantLeaseSummary;
import com.akandiah.propmanager.features.tenant.api.dto.TenantLeaseView;
import com.akandiah.propmanager.features.tenant.api.dto.TenantPropertySummary;
import com.akandiah.propmanager.features.tenant.api.dto.TenantUnitSummary;
import com.akandiah.propmanager.features.tenant.domain.Tenant;
import com.akandiah.propmanager.features.tenant.domain.TenantRepository;

import lombok.RequiredArgsConstructor;

/** Resolves the full lease context for a tenant-portal request (GET /api/tenant/my-lease). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TenantPortalService {

	private final TenantRepository tenantRepository;
	private final LeaseTenantRepository leaseTenantRepository;
	private final AssetRepository assetRepository;
	private final OrganizationRepository organizationRepository;

	/**
	 * Resolves the tenant's active lease context for the given org.
	 *
	 * @throws AccessDeniedException     if the caller is not a member of orgId
	 * @throws ResourceNotFoundException if the caller has no Tenant record, or no active lease in this org
	 */
	public TenantLeaseView getMyLease(UUID userId, UUID orgId) {
		// Step 1: verify org membership and load org for contact details
		Organization org = organizationRepository.findAllByUserId(userId).stream()
				.filter(o -> o.getId().equals(orgId))
				.findFirst()
				.orElseThrow(() -> new AccessDeniedException("Not a member of organization: " + orgId));

		// Step 2: resolve Tenant record
		Tenant tenant = tenantRepository.findByUser_Id(userId)
				.orElseThrow(() -> new ResourceNotFoundException("Tenant", userId));

		// Step 3: find active lease-tenant slots for this tenant in this org
		List<LeaseTenant> slots = leaseTenantRepository.findByTenantIdAndStatusInAndOrgId(
				tenant.getId(), LeaseStatus.ACTIVE_STATUSES, orgId);

		if (slots.isEmpty()) {
			throw new ResourceNotFoundException("active lease in org " + orgId, tenant.getId());
		}

		// Step 4: prefer ACTIVE; if multiple, use most recent startDate
		LeaseTenant bestSlot = slots.stream()
				.max(Comparator
						.comparingInt((LeaseTenant lt) -> lt.getLease().getStatus() == LeaseStatus.ACTIVE ? 1 : 0)
						.thenComparing(lt -> lt.getLease().getStartDate()))
				.orElseThrow();

		Lease lease = bestSlot.getLease();

		// Step 5: load assets for the unit
		List<Asset> assets = assetRepository.findByUnit_Id(lease.getUnit().getId());

		// Step 6: load all co-tenants for this lease (excluding the current tenant)
		List<LeaseTenant> allSlots = leaseTenantRepository.findByLeaseIdWithTenantAndInvite(lease.getId());
		List<TenantCoTenantSummary> coTenants = allSlots.stream()
				.filter(lt -> lt.getTenant() == null || !lt.getTenant().getId().equals(tenant.getId()))
				.map(this::toCoTenantSummary)
				.toList();

		return new TenantLeaseView(
				TenantLeaseSummary.from(lease),
				TenantPropertySummary.from(lease.getProperty(), org.getPhone(), org.getEmail()),
				TenantUnitSummary.from(lease.getUnit()),
				assets.stream().map(TenantAssetSummary::from).toList(),
				coTenants,
				bestSlot.getId(),
				bestSlot.getStatus());
	}

	private TenantCoTenantSummary toCoTenantSummary(LeaseTenant lt) {
		String name = (lt.getTenant() != null && lt.getTenant().getUser() != null)
				? lt.getTenant().getUser().getName()
				: lt.getInvite().getEmail();
		return new TenantCoTenantSummary(name, lt.getRole(), lt.getStatus());
	}
}
