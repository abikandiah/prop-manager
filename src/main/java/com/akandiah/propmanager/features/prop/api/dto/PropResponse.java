package com.akandiah.propmanager.features.prop.api.dto;

import java.time.Instant;

import com.akandiah.propmanager.features.prop.domain.Prop;

public record PropResponse(
		Long id,
		String name,
		String description,
		Instant createdAt,
		Instant updatedAt) {

	public static PropResponse from(Prop prop) {
		return new PropResponse(
				prop.getId(),
				prop.getName(),
				prop.getDescription(),
				prop.getCreatedAt(),
				prop.getUpdatedAt());
	}
}
