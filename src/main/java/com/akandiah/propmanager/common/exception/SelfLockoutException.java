package com.akandiah.propmanager.common.exception;

/** Thrown when a change would self-lock a user out or orphan an organization. */
public class SelfLockoutException extends RuntimeException {

	public SelfLockoutException() {
		super("This change would remove your access to this organization. Have another admin adjust your permissions.");
	}

	public SelfLockoutException(String message) {
		super(message);
	}
}
