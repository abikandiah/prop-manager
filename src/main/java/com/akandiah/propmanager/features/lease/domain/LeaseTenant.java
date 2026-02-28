package com.akandiah.propmanager.features.lease.domain;

import java.time.LocalDate;

import com.akandiah.propmanager.common.domain.BaseEntity;
import com.akandiah.propmanager.features.invite.domain.Invite;
import com.akandiah.propmanager.features.tenant.domain.Tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "lease_tenants")
@Getter
@SuperBuilder
@NoArgsConstructor
public class LeaseTenant extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "lease_id", nullable = false)
	private Lease lease;

	/**
	 * The invite that originated this tenant slot.
	 * Always set at creation; used to look up and fulfil the slot on invite acceptance.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "invite_id", nullable = false)
	private Invite invite;

	/**
	 * Null until the invited user accepts the invite and their tenant profile is created/linked.
	 */
	@Setter
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "tenant_id")
	private Tenant tenant;

	@Setter
	@Column(nullable = false, length = 32)
	@Enumerated(EnumType.STRING)
	private LeaseTenantRole role;

	@Setter
	@Column(name = "invited_date")
	private LocalDate invitedDate;

	@Setter
	@Column(name = "signed_date")
	private LocalDate signedDate;
}
