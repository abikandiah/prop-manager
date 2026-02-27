package com.akandiah.propmanager.features.tenant.domain;

import com.akandiah.propmanager.common.domain.BaseEntity;
import com.akandiah.propmanager.features.user.domain.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "tenants")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Tenant extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "emergency_contact_name", length = 255)
	private String emergencyContactName;

	@Column(name = "emergency_contact_phone", length = 50)
	private String emergencyContactPhone;

	@Column(name = "has_pets")
	private Boolean hasPets;

	@Column(name = "pet_description", columnDefinition = "text")
	private String petDescription;

	@Column(name = "vehicle_info", length = 255)
	private String vehicleInfo;

	@Column(name = "notes", columnDefinition = "text")
	private String notes;
}
