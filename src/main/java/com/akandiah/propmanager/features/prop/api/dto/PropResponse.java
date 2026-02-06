package com.akandiah.propmanager.features.prop.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.akandiah.propmanager.features.prop.domain.Prop;

public record PropResponse(
		UUID id,
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
