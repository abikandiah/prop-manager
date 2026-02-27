package com.akandiah.propmanager.features.lease.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.akandiah.propmanager.common.domain.BaseEntity;
import com.akandiah.propmanager.features.prop.domain.Prop;
import com.akandiah.propmanager.features.unit.domain.Unit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "leases")
@Getter
@SuperBuilder
@NoArgsConstructor
public class Lease extends BaseEntity {

	// Optional FK — may be null if the template was deleted after stamping.
	@Setter
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "lease_template_id")
	private LeaseTemplate leaseTemplate;

	// Denormalized snapshot so the lease carries its own provenance
	// even after the template is deleted.
	@Setter
	@Column(name = "lease_template_name", length = 255)
	private String leaseTemplateName;

	@Setter
	@Column(name = "lease_template_version_tag", length = 50)
	private String leaseTemplateVersionTag;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "unit_id", nullable = false)
	private Unit unit;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "property_id", nullable = false)
	private Prop property;

	@Setter
	@Column(nullable = false, length = 32)
	@Enumerated(EnumType.STRING)
	private LeaseStatus status;

	@Setter
	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Setter
	@Column(name = "end_date", nullable = false)
	private LocalDate endDate;

	@Setter
	@Column(name = "rent_amount", precision = 19, scale = 4, nullable = false)
	private BigDecimal rentAmount;

	@Setter
	@Lob
	@Column(name = "executed_content_markdown", columnDefinition = "TEXT")
	private String executedContentMarkdown;

	@Setter
	@Column(name = "rent_due_day", nullable = false)
	private Integer rentDueDay;

	@Setter
	@Column(name = "security_deposit_held", precision = 19, scale = 4)
	private BigDecimal securityDepositHeld;

	@Setter
	@Column(name = "late_fee_type", length = 32)
	@Enumerated(EnumType.STRING)
	private LateFeeType lateFeeType;

	@Setter
	@Column(name = "late_fee_amount", precision = 19, scale = 4)
	private BigDecimal lateFeeAmount;

	@Setter
	@Column(name = "notice_period_days")
	private Integer noticePeriodDays;

	@Setter
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "additional_metadata")
	private Map<String, Object> additionalMetadata;

	@Setter
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "template_parameters")
	private Map<String, String> templateParameters;
}
