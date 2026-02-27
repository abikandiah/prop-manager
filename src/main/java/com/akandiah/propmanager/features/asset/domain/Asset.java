package com.akandiah.propmanager.features.asset.domain;

import java.time.LocalDate;

import com.akandiah.propmanager.common.domain.BaseEntity;
import com.akandiah.propmanager.features.prop.domain.Prop;
import com.akandiah.propmanager.features.unit.domain.Unit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "assets")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Asset extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "property_id")
	private Prop prop;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "unit_id")
	private Unit unit;

	@Column(nullable = false, length = 32)
	@Enumerated(EnumType.STRING)
	private AssetCategory category;

	@Column(name = "make_model", length = 255)
	private String makeModel;

	@Column(name = "serial_number", length = 128)
	private String serialNumber;

	@Column(name = "install_date")
	private LocalDate installDate;

	@Column(name = "warranty_expiry")
	private LocalDate warrantyExpiry;

	@Column(name = "last_service_date")
	private LocalDate lastServiceDate;
}
