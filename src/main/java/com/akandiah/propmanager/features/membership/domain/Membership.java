package com.akandiah.propmanager.features.membership.domain;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.akandiah.propmanager.features.invite.domain.Invite;
import com.akandiah.propmanager.features.organization.domain.Organization;
import com.akandiah.propmanager.features.user.domain.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Bridge between User and Organization. One row per (user, org).
 * If user is null, it represents a pending invitation slot.
 */
@Entity
@Table(name = "memberships", uniqueConstraints = {
		@UniqueConstraint(name = "uk_memberships_user_org", columnNames = { "user_id", "org_id" })
}, indexes = {
		@Index(name = "idx_memberships_user_id", columnList = "user_id"),
		@Index(name = "idx_memberships_org_id", columnList = "org_id"),
		@Index(name = "idx_memberships_invite_id", columnList = "invite_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Membership {

	@Id
	@UuidGenerator(style = UuidGenerator.Style.TIME)
	private UUID id;

	/**
	 * Null until the invited user accepts the invite.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "org_id", nullable = false)
	private Organization organization;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "invite_id")
	private Invite invite;

	@Version
	@Column(nullable = false)
	private Integer version;

	@Column(name = "created_at", nullable = false, updatable = false)
	@Setter(AccessLevel.NONE)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	@Setter(AccessLevel.NONE)
	private Instant updatedAt;

	@PrePersist
	void prePersist() {
		Instant now = Instant.now();
		if (createdAt == null) {
			createdAt = now;
		}
		updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = Instant.now();
	}
}
