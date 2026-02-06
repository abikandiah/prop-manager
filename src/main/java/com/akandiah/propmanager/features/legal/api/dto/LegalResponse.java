package com.akandiah.propmanager.features.legal.api.dto;

import java.time.OffsetDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response containing legal document content")
public record LegalResponse(
		@Schema(description = "The title of the document") String title,
		@Schema(description = "The content of the document (Markdown or HTML format)") String content,
		@Schema(description = "The last updated timestamp") OffsetDateTime lastUpdated) {
}
