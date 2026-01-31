package com.akandiah.propmanager.features.prop.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "prop")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Prop {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 255)
	private String name;

	@Column(columnDefinition = "text")
	private String description;

	@Column(name = "created_at", nullable = false, updatable = false)
	@Setter(AccessLevel.NONE)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@jakarta.persistence.PrePersist
	void prePersist() {
		Instant now = Instant.now();
		if (createdAt == null)
			createdAt = now;
		updatedAt = now;
	}

	@jakarta.persistence.PreUpdate
	void preUpdate() {
		updatedAt = Instant.now();
	}
}
