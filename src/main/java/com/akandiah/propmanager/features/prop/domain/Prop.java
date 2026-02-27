package com.akandiah.propmanager.features.prop.domain;

import java.util.UUID;

import com.akandiah.propmanager.common.domain.BaseEntity;
import com.akandiah.propmanager.features.organization.domain.Organization;

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
@Table(name = "prop")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Prop extends BaseEntity {

	@Column(name = "legal_name", nullable = false, length = 255)
	private String legalName;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "address_id", nullable = false)
	private Address address;

	@Column(name = "property_type", nullable = false, length = 32)
	@Enumerated(EnumType.STRING)
	private PropertyType propertyType;

	@Column(name = "description", length = 2000)
	private String description;

	@Column(name = "parcel_number", length = 64)
	private String parcelNumber;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "organization_id", nullable = false)
	private Organization organization;

	@Column(name = "owner_id", nullable = false)
	private UUID ownerId;

	@Column(name = "total_area")
	private Integer totalArea;

	@Column(name = "year_built")
	private Integer yearBuilt;
}
