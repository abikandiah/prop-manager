package com.akandiah.propmanager.features.organization.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import com.akandiah.propmanager.common.permission.ResourceType;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

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
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Granular scope for a membership: (scopeType, scopeId) → permissions within the org. */
@Entity
@Table(name = "member_scopes", uniqueConstraints = {
		@UniqueConstraint(name = "uk_member_scopes_membership_scope", columnNames = { "membership_id", "scope_type", "scope_id" })
}, indexes = {
		@Index(name = "idx_member_scopes_membership_id", columnList = "membership_id"),
		@Index(name = "idx_member_scopes_scope", columnList = "scope_type, scope_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberScope {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.TIME)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "membership_id", nullable = false)
	private Membership membership;

	@Column(name = "scope_type", nullable = false, length = 32)
	@Enumerated(EnumType.STRING)
	private ResourceType scopeType;

	@Column(name = "scope_id", nullable = false)
	private UUID scopeId;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "permissions", nullable = false)
	@Builder.Default
	private Map<String, String> permissions = Collections.emptyMap();

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
