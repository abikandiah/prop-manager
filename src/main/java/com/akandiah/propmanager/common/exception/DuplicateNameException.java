package com.akandiah.propmanager.common.exception;

/** Thrown when a name uniqueness constraint would be violated. Mapped to 409 Conflict. */
public class DuplicateNameException extends RuntimeException {

	private final String field;

	public DuplicateNameException(String field, String value) {
		super("'" + value + "' is already in use.");
		this.field = field;
	}

	public String getField() {
		return field;
	}
}
