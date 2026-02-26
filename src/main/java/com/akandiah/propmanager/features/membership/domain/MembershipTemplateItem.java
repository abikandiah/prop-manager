package com.akandiah.propmanager.features.membership.domain;

import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.akandiah.propmanager.common.permission.ResourceType;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One scope-level entry within a {@link MembershipTemplate}.
 * <p>
 * {@code scopeType} = the hierarchy level at which this grant lives (ORG, PROPERTY, UNIT).
 * <p>
 * {@code permissions} = domain key → action letters for that level (e.g. {@code {"l":"cru","m":"r"}}).
 * <p>
 * ORG items activate unconditionally when the template is applied to a membership.
 * PROPERTY/UNIT items activate only when a {@link MemberScope} binding row exists for that resource.
 */
@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipTemplateItem {

	@Enumerated(EnumType.STRING)
	@Column(name = "scope_type", nullable = false, length = 32)
	private ResourceType scopeType;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "permissions", nullable = false)
	private Map<String, String> permissions;
}
