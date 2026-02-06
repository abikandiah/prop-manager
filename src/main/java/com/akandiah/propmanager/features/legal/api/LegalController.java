package com.akandiah.propmanager.features.legal.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.akandiah.propmanager.features.legal.api.dto.LegalResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/public")
@Tag(name = "Legal", description = "Public legal documents (Terms, Privacy)")
@RequiredArgsConstructor
@Slf4j
public class LegalController {

	@Value("classpath:legal/terms.md")
	private Resource termsResource;

	@Value("classpath:legal/privacy.md")
	private Resource privacyResource;

	@GetMapping("/terms")
	@Operation(summary = "Get Terms and Conditions", description = "Returns the current terms and conditions for the application.")
	public LegalResponse getTerms() {
		return loadLegalDocument("Terms and Conditions", termsResource);
	}

	@GetMapping("/privacy")
	@Operation(summary = "Get Privacy Policy", description = "Returns the current privacy policy for the application.")
	public LegalResponse getPrivacy() {
		return loadLegalDocument("Privacy Policy", privacyResource);
	}

	private LegalResponse loadLegalDocument(String title, Resource resource) {
		try {
			String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
			long lastModified = resource.lastModified();
			OffsetDateTime lastUpdated = OffsetDateTime.ofInstant(Instant.ofEpochMilli(lastModified), ZoneOffset.UTC);

			return new LegalResponse(title, content, lastUpdated);
		} catch (IOException e) {
			log.error("Failed to load legal document: {}", title, e);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not load legal document");
		}
	}
}
