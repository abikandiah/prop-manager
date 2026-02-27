package com.akandiah.propmanager.features.organization.domain;

import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.akandiah.propmanager.common.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "organizations")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Organization extends BaseEntity {

	@Column(nullable = false, length = 255)
	private String name;

	@Column(name = "tax_id", length = 64)
	private String taxId;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "settings")
	private Map<String, Object> settings;
}
