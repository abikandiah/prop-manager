package com.akandiah.propmanager.features.lease.domain;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;

import org.hibernate.annotations.UuidGenerator;

import com.akandiah.propmanager.features.tenant.domain.Tenant;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lease_tenants")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaseTenant {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.TIME)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "lease_id", nullable = false)
	private Lease lease;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "tenant_id", nullable = false)
	private Tenant tenant;

	@Column(nullable = false, length = 32)
	@Enumerated(EnumType.STRING)
	private LeaseTenantRole role;

	@Column(name = "invited_date")
	private LocalDate invitedDate;

	@Column(name = "signed_date")
	private LocalDate signedDate;
}
