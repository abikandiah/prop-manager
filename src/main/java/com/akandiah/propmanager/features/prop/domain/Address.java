package com.akandiah.propmanager.features.prop.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.TIME)
	private UUID id;

	@Column(name = "address_line_1", nullable = false, length = 255)
	private String addressLine1;

	@Column(name = "address_line_2", length = 255)
	private String addressLine2;

	@Column(nullable = false, length = 100)
	private String city;

	@Column(name = "state_province_region", nullable = false, length = 100)
	private String stateProvinceRegion;

	@Column(name = "postal_code", nullable = false, length = 20)
	private String postalCode;

	@Column(name = "country_code", nullable = false, length = 2)
	private String countryCode;

	@Column(precision = 9, scale = 6)
	private BigDecimal latitude;

	@Column(precision = 9, scale = 6)
	private BigDecimal longitude;

	@Column(name = "created_at", nullable = false, updatable = false)
	@Setter(AccessLevel.NONE)
	private Instant createdAt;

	@jakarta.persistence.PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}
}
