package com.akandiah.propmanager.features.invite.service;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.features.invite.domain.InviteAcceptedEvent;
import com.akandiah.propmanager.features.invite.domain.TargetType;
import com.akandiah.propmanager.features.lease.domain.LeaseTenant;
import com.akandiah.propmanager.features.lease.domain.LeaseTenantRepository;
import com.akandiah.propmanager.features.tenant.domain.Tenant;
import com.akandiah.propmanager.features.tenant.domain.TenantRepository;
import com.akandiah.propmanager.features.user.domain.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles the post-acceptance side effects for tenant invites.
 *
 * <p>
 * On accept: finds or creates the user's Tenant profile, then links it to
 * the LeaseTenant slot that was created when the invite was sent.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TenantInviteAcceptedListener {

	private final TenantRepository tenantRepository;
	private final LeaseTenantRepository leaseTenantRepository;

	@EventListener
	@Transactional
	public void onInviteAccepted(InviteAcceptedEvent event) {
		if (event.invite().getTargetType() != TargetType.LEASE) {
			return;
		}

		User claimedBy = event.claimedUser();

		Tenant tenant = tenantRepository.findByUser_Id(claimedBy.getId())
				.orElseGet(() -> {
					log.info("Creating new Tenant profile for user id={}", claimedBy.getId());
					return tenantRepository.save(Tenant.builder().user(claimedBy).build());
				});

		LeaseTenant leaseTenant = leaseTenantRepository.findByInvite_Id(event.invite().getId())
				.orElseThrow(() -> new IllegalStateException(
						"No LeaseTenant slot found for invite id=" + event.invite().getId()));

		leaseTenant.setTenant(tenant);
		leaseTenantRepository.save(leaseTenant);

		log.info("Linked tenant id={} to lease_tenant id={} via invite id={}",
				tenant.getId(), leaseTenant.getId(), event.invite().getId());
	}
}
