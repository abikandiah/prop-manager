package com.akandiah.propmanager.features.lease.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.akandiah.propmanager.features.invite.domain.Invite;
import com.akandiah.propmanager.features.tenant.domain.Tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lease_tenants")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LeaseTenant {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.TIME)
	private UUID id;

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

	@Version
	@Column(nullable = false)
	private Integer version;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@PrePersist
	void prePersist() {
		Instant now = Instant.now();
		createdAt = (createdAt == null) ? now : createdAt;
		updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = Instant.now();
	}
}
