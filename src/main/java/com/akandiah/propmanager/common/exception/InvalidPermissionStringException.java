package com.akandiah.propmanager.common.exception;

import java.util.List;

/**
 * Thrown when a permissions map (domain key → action letters) fails validation.
 * Handled by {@link GlobalExceptionHandler} → 400 with RFC 7807 {@code errors} array.
 */
public class InvalidPermissionStringException extends RuntimeException {

	private final List<PermissionValidationError> errors;

	public InvalidPermissionStringException(List<PermissionValidationError> errors) {
		super(errors != null && !errors.isEmpty()
				? errors.get(0).message()
				: "Invalid permissions");
		this.errors = errors != null ? List.copyOf(errors) : List.of();
	}

	public List<PermissionValidationError> getErrors() {
		return errors;
	}

	public record PermissionValidationError(String field, String message) {
	}
}
