package com.akandiah.propmanager.features.lease.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
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
@Table(name = "lease_templates")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaseTemplate {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.TIME)
	private UUID id;

	@Column(nullable = false, length = 255)
	private String name; // e.g., "Ontario Residential Standard 2026"

	@Column(name = "version_tag")
	private String versionTag; // e.g., "v2.1"

	/**
	 * JPA optimistic-lock version. Auto-incremented on every update;
	 * prevents concurrent edits and gives each save a monotonic revision number.
	 */
	@Version
	@Column(nullable = false)
	private Integer version;

	// The master text with placeholders like {{tenant_name}}, {{rent_amount}}
	@Lob
	@Column(name = "template_markdown", columnDefinition = "TEXT", nullable = false)
	private String templateMarkdown;

	@Column(name = "default_late_fee_type", length = 32)
	@Enumerated(EnumType.STRING)
	private LateFeeType defaultLateFeeType;

	@Column(name = "default_late_fee_amount", precision = 19, scale = 4)
	private BigDecimal defaultLateFeeAmount;

	@Column(name = "default_notice_period_days")
	private Integer defaultNoticePeriodDays;

	/**
	 * Whether this template can be used for new leases (enable/disable).
	 */
	@Builder.Default
	@Column(name = "is_active", nullable = false)
	private boolean active = true;

	/**
	 * Default placeholder values for this template. When stamping a lease,
	 * these are applied after built-in params and can be overridden by the
	 * lease request's templateParameters.
	 */
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "template_parameters")
	private Map<String, String> templateParameters;

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
