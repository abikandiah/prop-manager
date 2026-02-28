package com.akandiah.propmanager.features.membership.domain;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.akandiah.propmanager.common.domain.BaseEntity;
import com.akandiah.propmanager.features.organization.domain.Organization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Flat permission policy — a named set of domain-key → action-letter permissions.
 * <p>
 * System-wide policies have {@code org == null}. Org-specific policies have {@code org} set.
 * <p>
 * Permissions are a flat map (no scope type embedded) — the resource level is determined
 * at assignment time by {@link PolicyAssignment}.
 */
@Entity
@Table(name = "permission_policies", uniqueConstraints = {
		@UniqueConstraint(name = "uk_permission_policies_org_name", columnNames = { "org_id", "name" })
}, indexes = {
		@Index(name = "idx_permission_policies_org_id", columnList = "org_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class PermissionPolicy extends BaseEntity {

	/** null = system-wide policy; non-null = org-specific policy. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "org_id")
	private Organization org;

	@Column(name = "name", nullable = false, columnDefinition = "text")
	private String name;

	/** Domain key → action letters (e.g. {@code {"l":"cru","m":"r"}}). */
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "permissions", nullable = false)
	@Builder.Default
	private Map<String, String> permissions = new HashMap<>();
}
