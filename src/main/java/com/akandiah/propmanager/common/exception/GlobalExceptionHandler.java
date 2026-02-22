package com.akandiah.propmanager.common.exception;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
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

import com.akandiah.propmanager.common.notification.NotificationException;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	private static final HttpStatusCode STATUS_UNPROCESSABLE_ENTITY = HttpStatusCode.valueOf(422);

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

	/** 400: Invalid arguments (e.g. mutually exclusive fields) */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex,
			HttpServletRequest request) {
		return problem(HttpStatus.BAD_REQUEST, "Bad Request",
				ex.getMessage(), request.getRequestURI(), null, null);
	}

	/** 400: Invalid permissions map (domain key or action letters) */
	@ExceptionHandler(InvalidPermissionStringException.class)
	public ResponseEntity<ProblemDetail> handleInvalidPermissionString(InvalidPermissionStringException ex,
			HttpServletRequest request) {
		List<FieldErrorDto> errors = ex.getErrors().stream()
				.map(e -> new FieldErrorDto(e.field(), e.message()))
				.collect(Collectors.toList());
		return problem(HttpStatus.BAD_REQUEST, "Validation Failed", "Invalid permissions", request.getRequestURI(),
				errors, null);
	}

	/** 422: Cannot delete because entity has child records */
	@ExceptionHandler(HasChildrenException.class)
	public ResponseEntity<ProblemDetail> handleHasChildren(HasChildrenException ex, HttpServletRequest request) {
		ProblemDetail body = ProblemDetail.forStatusAndDetail(STATUS_UNPROCESSABLE_ENTITY, ex.getMessage());
		body.setTitle("Cannot Delete");
		body.setInstance(URI.create(request.getRequestURI()));
		body.setProperty("parentName", ex.getParentName());
		body.setProperty("parentId", ex.getParentId().toString());
		body.setProperty("childCount", ex.getChildCount());
		body.setProperty("childLabel", ex.getChildLabel());
		return ResponseEntity.status(STATUS_UNPROCESSABLE_ENTITY).body(body);
	}

	/** 422: Illegal state transition (e.g. editing a non-DRAFT lease) */
	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ProblemDetail> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
		return problem(STATUS_UNPROCESSABLE_ENTITY, "Unprocessable Entity",
				ex.getMessage(), request.getRequestURI(), null, null);
	}

	/** 409: Optimistic Lock Conflict */
	@ExceptionHandler(OptimisticLockException.class)
	public ResponseEntity<ProblemDetail> handleOptimisticLock(OptimisticLockException ex, HttpServletRequest request) {
		return problem(HttpStatus.CONFLICT, "Conflict",
				ex.getMessage() != null ? ex.getMessage()
						: "The resource was modified by another request. Please refresh and try again.",
				request.getRequestURI(), null, null);
	}

	/** 409: Data Integrity Violation (e.g. FK constraint, concurrent insert) */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ProblemDetail> handleDataIntegrityViolation(DataIntegrityViolationException ex,
			HttpServletRequest request) {
		log.warn("Data integrity violation at {}: {}", request.getRequestURI(), ex.getMessage());
		return problem(HttpStatus.CONFLICT, "Conflict",
				"Operation failed due to a referential integrity constraint. A related record may have been created concurrently.",
				request.getRequestURI(), null, null);
	}

	/** 503: Notification delivery failed (e.g. email send or template render) */
	@ExceptionHandler(NotificationException.class)
	public ResponseEntity<ProblemDetail> handleNotification(NotificationException ex, HttpServletRequest request) {
		log.warn("Notification failed at {}: {}", request.getRequestURI(), ex.getMessage());
		return problem(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable",
				"Notification could not be sent. Please try again later.", request.getRequestURI(), null, null);
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
		return problem((HttpStatusCode) status, title, detail, path, fieldErrors, debugInfo);
	}

	private ResponseEntity<ProblemDetail> problem(
			HttpStatusCode status,
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