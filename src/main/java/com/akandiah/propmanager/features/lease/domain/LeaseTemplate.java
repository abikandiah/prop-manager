package com.akandiah.propmanager.features.lease.domain;

import java.math.BigDecimal;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.akandiah.propmanager.common.domain.BaseEntity;
import com.akandiah.propmanager.features.organization.domain.Organization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "lease_templates")
@Getter
@SuperBuilder
@NoArgsConstructor
public class LeaseTemplate extends BaseEntity {

	@Setter
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "org_id", nullable = false)
	private Organization org;

	@Setter
	@Column(nullable = false, length = 255)
	private String name;

	@Setter
	@Column(name = "version_tag")
	private String versionTag;

	@Setter
	@Lob
	@Column(name = "template_markdown", columnDefinition = "TEXT", nullable = false)
	private String templateMarkdown;

	@Setter
	@Column(name = "default_late_fee_type", length = 32)
	@Enumerated(EnumType.STRING)
	private LateFeeType defaultLateFeeType;

	@Setter
	@Column(name = "default_late_fee_amount", precision = 19, scale = 4)
	private BigDecimal defaultLateFeeAmount;

	@Setter
	@Column(name = "default_notice_period_days")
	private Integer defaultNoticePeriodDays;

	@Setter
	@Builder.Default
	@Column(name = "is_active", nullable = false)
	private boolean active = true;

	@Setter
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "template_parameters")
	private Map<String, String> templateParameters;
}
