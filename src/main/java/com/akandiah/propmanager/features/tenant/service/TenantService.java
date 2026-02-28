package com.akandiah.propmanager.features.tenant.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.common.exception.ResourceNotFoundException;
import com.akandiah.propmanager.common.permission.AccessListUtil.ScopedAccessFilter;
import com.akandiah.propmanager.common.util.OptimisticLockingUtil;
import com.akandiah.propmanager.features.tenant.api.dto.TenantResponse;
import com.akandiah.propmanager.features.tenant.api.dto.UpdateTenantRequest;
import com.akandiah.propmanager.features.tenant.domain.Tenant;
import com.akandiah.propmanager.features.tenant.domain.TenantRepository;
import com.akandiah.propmanager.features.user.domain.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TenantService {

	private final TenantRepository tenantRepository;

	// ───────────────────────── Queries ─────────────────────────

	/** Returns all tenants (admin use only — no scope filtering). */
	public List<TenantResponse> findAll() {
		return tenantRepository.findAll().stream()
				.map(TenantResponse::from)
				.toList();
	}

	/**
	 * Returns tenants visible to the caller based on their scoped lease access.
	 * Returns an empty list immediately when the filter is empty (caller has no
	 * lease-read permissions), avoiding an unnecessary full-table scan.
	 *
	 * @param filter     the caller's authorized org/prop/unit scope
	 * @param activeOnly when true, only tenants on ACTIVE or REVIEW leases are returned
	 */
	public List<TenantResponse> findAll(ScopedAccessFilter filter, boolean activeOnly) {
		if (filter.isEmpty()) return List.of();
		return tenantRepository
				.findByAccessFilter(filter.orgIds(), filter.propIds(), filter.unitIds(), activeOnly)
				.stream()
				.map(TenantResponse::from)
				.toList();
	}

	public TenantResponse findById(UUID id) {
		Tenant tenant = tenantRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Tenant", id));
		return TenantResponse.from(tenant);
	}

	/**
	 * Returns the tenant profile for the given user, or empty if they have not
	 * yet accepted a lease invite (profile is created on invite acceptance).
	 */
	public Optional<TenantResponse> findByUser(User user) {
		return tenantRepository.findByUser_Id(user.getId())
				.map(TenantResponse::from);
	}

	// ───────────────────────── Update ─────────────────────────

	/**
	 * Updates rental-context fields on the current user's tenant profile.
	 * Returns 404 if the user has no tenant profile yet.
	 */
	@Transactional
	public TenantResponse updateByUser(User user, UpdateTenantRequest req) {
		Tenant tenant = tenantRepository.findByUser_Id(user.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Tenant profile", user.getId()));

		OptimisticLockingUtil.requireVersionMatch("Tenant", tenant.getId(), tenant.getVersion(), req.version());

		if (req.emergencyContactName() != null) {
			tenant.setEmergencyContactName(req.emergencyContactName());
		}
		if (req.emergencyContactPhone() != null) {
			tenant.setEmergencyContactPhone(req.emergencyContactPhone());
		}
		if (req.hasPets() != null) {
			tenant.setHasPets(req.hasPets());
		}
		if (req.petDescription() != null) {
			tenant.setPetDescription(req.petDescription());
		}
		if (req.vehicleInfo() != null) {
			tenant.setVehicleInfo(req.vehicleInfo());
		}
		if (req.notes() != null) {
			tenant.setNotes(req.notes());
		}

		return TenantResponse.from(tenantRepository.save(tenant));
	}
}
