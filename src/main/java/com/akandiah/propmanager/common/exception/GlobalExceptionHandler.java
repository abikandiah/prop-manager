package com.akandiah.propmanager.common.exception;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@Value("${spring.profiles.active:default}")
	private String activeProfile;

	/** 404: Custom Resource Not Found */
	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ProblemDetail> handleResourceNotFound(ResourceNotFoundException ex,
			HttpServletRequest request) {
		return problem(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request.getRequestURI(), null, null);
	}

	/** 400: Validation Errors (JSON Body) */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		List<FieldErrorDto> errors = ex.getBindingResult().getFieldErrors().stream()
				.map(e -> new FieldErrorDto(e.getField(), e.getDefaultMessage()))
				.collect(Collectors.toList());
		return problem(HttpStatus.BAD_REQUEST, "Validation Failed", "Invalid request body", request.getRequestURI(),
				errors, null);
	}

	/** 400: Binding Errors (Query Params/Forms) */
	@ExceptionHandler(BindException.class)
	public ResponseEntity<ProblemDetail> handleBind(BindException ex, HttpServletRequest request) {
		List<FieldErrorDto> errors = ex.getFieldErrors().stream()
				.map(e -> new FieldErrorDto(e.getField(),
						e.getDefaultMessage() != null ? e.getDefaultMessage() : "Invalid"))
				.collect(Collectors.toList());
		return problem(HttpStatus.BAD_REQUEST, "Bad Request", "Invalid request parameters", request.getRequestURI(),
				errors, null);
	}

	/** 400: Malformed JSON */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ProblemDetail> handleNotReadable(HttpMessageNotReadableException ex,
			HttpServletRequest request) {
		log.debug("Malformed JSON request: {}", ex.getMessage());
		return problem(HttpStatus.BAD_REQUEST, "Bad Request", "Malformed or invalid request body",
				request.getRequestURI(), null, null);
	}

	/** 403: Security Access Denied */
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
		return problem(HttpStatus.FORBIDDEN, "Forbidden", "You do not have permission to access this resource",
				request.getRequestURI(), null, null);
	}

	/** 401: Authentication Failures */
	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
		return problem(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage(), request.getRequestURI(), null, null);
	}

	/** Handle ResponseStatusException from Spring */
	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ProblemDetail> handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
		HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
		String reason = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
		return problem(status, reason, reason, request.getRequestURI(), null, null);
	}

	/** 500: Catch-all for Unhandled Exceptions */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ProblemDetail> handleGeneric(Exception ex, HttpServletRequest request) {
		log.error("Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

		String stackTrace = "dev".equalsIgnoreCase(activeProfile)
				? Arrays.toString(ex.getStackTrace())
				: null;

		return problem(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"Internal Server Error",
				"An unexpected error occurred.",
				request.getRequestURI(),
				null,
				stackTrace);
	}

	/** Utility to build RFC 7807 ProblemDetail responses */
	private ResponseEntity<ProblemDetail> problem(
			HttpStatus status,
			String title,
			String detail,
			String path,
			List<FieldErrorDto> fieldErrors,
			String debugInfo) {

		ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail != null ? detail : "");
		body.setTitle(title);
		body.setInstance(URI.create(path));

		if (fieldErrors != null && !fieldErrors.isEmpty()) {
			body.setProperty("errors", fieldErrors);
		}

		if (debugInfo != null) {
			body.setProperty("debug_stacktrace", debugInfo);
		}

		return ResponseEntity.status(status).body(body);
	}

	public record FieldErrorDto(String field, String message) {
	}
}