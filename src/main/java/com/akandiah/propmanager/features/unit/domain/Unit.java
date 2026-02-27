package com.akandiah.propmanager.features.unit.domain;

import java.math.BigDecimal;

import com.akandiah.propmanager.common.domain.BaseEntity;
import com.akandiah.propmanager.features.prop.domain.Prop;

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
@Table(name = "units")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Unit extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "property_id", nullable = false)
	private Prop prop;

	@Column(name = "unit_number", nullable = false, length = 64)
	private String unitNumber;

	@Column(nullable = false, length = 32)
	@Enumerated(EnumType.STRING)
	private UnitStatus status;

	@Column(name = "unit_type", length = 32)
	@Enumerated(EnumType.STRING)
	private UnitType unitType;

	@Column(name = "description", length = 2000)
	private String description;

	@Column(name = "rent_amount", precision = 19, scale = 4)
	private BigDecimal rentAmount;

	@Column(name = "security_deposit", precision = 19, scale = 4)
	private BigDecimal securityDeposit;

	@Column(name = "bedrooms")
	private Integer bedrooms;

	@Column(name = "bathrooms")
	private Integer bathrooms;

	@Column(name = "square_footage")
	private Integer squareFootage;

	@Column(name = "balcony")
	private Boolean balcony;

	@Column(name = "laundry_in_unit")
	private Boolean laundryInUnit;

	@Column(name = "hardwood_floors")
	private Boolean hardwoodFloors;
}
