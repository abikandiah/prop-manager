package com.akandiah.propmanager.features.unit.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.akandiah.propmanager.features.prop.domain.Prop;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "units")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Unit {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.TIME)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "property_id", nullable = false)
	private Prop prop;

	@Column(name = "unit_number", nullable = false, length = 64)
	private String unitNumber;

	@Column(nullable = false, length = 32)
	@Enumerated(EnumType.STRING)
	private UnitStatus status;

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
