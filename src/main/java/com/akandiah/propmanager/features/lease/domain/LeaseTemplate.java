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
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LeaseTemplate {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.TIME)
	private UUID id;

	@Setter
	@Column(nullable = false, length = 255)
	private String name;

	@Setter
	@Column(name = "version_tag")
	private String versionTag;

	@Version
	@Column(nullable = false)
	private Integer version;

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
