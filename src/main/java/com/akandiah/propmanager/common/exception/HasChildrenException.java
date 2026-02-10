package com.akandiah.propmanager.common.exception;

import java.util.UUID;

/**
 * Thrown when a delete is rejected because the entity has child records that must be removed first.
 */
public class HasChildrenException extends RuntimeException {

	private final String parentName;
	private final UUID parentId;
	private final long childCount;
	private final String childLabel;
	private final String action;

	public HasChildrenException(String parentName, UUID parentId, long childCount,
			String childLabel, String action) {
		super(String.format("Cannot delete %s %s: it has %d %s. %s",
				parentName, parentId, childCount, childLabel, action));
		this.parentName = parentName;
		this.parentId = parentId;
		this.childCount = childCount;
		this.childLabel = childLabel;
		this.action = action;
	}

	public String getParentName() {
		return parentName;
	}

	public UUID getParentId() {
		return parentId;
	}

	public long getChildCount() {
		return childCount;
	}

	public String getChildLabel() {
		return childLabel;
	}

	public String getAction() {
		return action;
	}
}
