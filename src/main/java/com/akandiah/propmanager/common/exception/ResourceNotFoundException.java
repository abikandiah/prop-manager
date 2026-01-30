package com.akandiah.propmanager.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception to be thrown when a requested resource (Property, User,
 * etc.)
 * is not found in the database.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException(String message) {
		super(message);
	}

	/**
	 * Helper constructor for common "Resource with ID X not found" messages.
	 */
	public ResourceNotFoundException(String resourceName, Object id) {
		super(String.format("%s with id [%s] not found", resourceName, id));
	}
}