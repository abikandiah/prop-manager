package com.akandiah.propmanager.features.invite.domain;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.akandiah.propmanager.features.user.domain.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Generic invitation entity for inviting users to join various resources
 * (Lease, Property, Company, etc.) with specific roles.
 */
@Entity
@Table(name = "invite", indexes = {
		@Index(name = "idx_invite_token", columnList = "token", unique = true),
		@Index(name = "idx_invite_email", columnList = "email"),
		@Index(name = "idx_invite_target", columnList = "target_type, target_id"),
		@Index(name = "idx_invite_status", columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invite {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.TIME)
	private UUID id;

	@Column(nullable = false, length = 320) // Max email length per RFC 5321
	private String email;

	@Column(nullable = false, unique = true, length = 64)
	private String token;

	@Enumerated(EnumType.STRING)
	@Column(name = "target_type", nullable = false, length = 32)
	private TargetType targetType;

	@Column(name = "target_id", nullable = false)
	private UUID targetId;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "attributes", columnDefinition = "jsonb")
	@Builder.Default
	private Map<String, Object> attributes = new HashMap<>();

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "invited_by_id", nullable = false)
	private User invitedBy;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	@Builder.Default
	private InviteStatus status = InviteStatus.PENDING;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "sent_at")
	private Instant sentAt;

	@Column(name = "last_resent_at")
	private Instant lastResentAt;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "accepted_at")
	private Instant acceptedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "claimed_user_id")
	private User claimedUser;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@PrePersist
	protected void onCreate() {
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = Instant.now();
	}

	public boolean isValid() {
		return status == InviteStatus.PENDING && Instant.now().isBefore(expiresAt);
	}

	public boolean isExpired() {
		return Instant.now().isAfter(expiresAt);
	}
}
