package com.akandiah.propmanager.features.membership.domain;

import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.akandiah.propmanager.common.domain.BaseEntity;
import com.akandiah.propmanager.common.permission.ResourceType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Direct, explicit permission assignment for a membership at a specific resource.
 * <p>
 * Each assignment says: "this member has [policy] permissions on [resourceType] [resourceId]."
 * Optionally, the assignment carries {@code overrides} instead of or in addition to a policy.
 * If both are present, {@code overrides} takes precedence.
 */
@Entity
@Table(name = "policy_assignments", uniqueConstraints = {
		@UniqueConstraint(name = "uk_policy_assignments_membership_resource",
				columnNames = { "membership_id", "resource_type", "resource_id" })
}, indexes = {
		@Index(name = "idx_policy_assignments_membership_id", columnList = "membership_id"),
		@Index(name = "idx_policy_assignments_resource", columnList = "resource_type, resource_id"),
		@Index(name = "idx_policy_assignments_policy_id", columnList = "policy_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class PolicyAssignment extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "membership_id", nullable = false)
	private Membership membership;

	@Enumerated(EnumType.STRING)
	@Column(name = "resource_type", nullable = false, length = 32)
	private ResourceType resourceType;

	@Column(name = "resource_id", nullable = false)
	private UUID resourceId;

	/** Optional policy reference — null for purely custom assignments. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "policy_id")
	private PermissionPolicy policy;

	/**
	 * Optional custom permission overrides. When present, these take precedence over
	 * the linked policy permissions.
	 */
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "overrides")
	private Map<String, String> overrides;
}
