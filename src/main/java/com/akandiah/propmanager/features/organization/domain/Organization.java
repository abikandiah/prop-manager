package com.akandiah.propmanager.features.organization.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
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
@Table(name = "organizations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Organization {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.TIME)
	private UUID id;

	@Column(nullable = false, length = 255)
	private String name;

	@Column(name = "tax_id", length = 64)
	private String taxId;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "settings")
	private Map<String, Object> settings;

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
