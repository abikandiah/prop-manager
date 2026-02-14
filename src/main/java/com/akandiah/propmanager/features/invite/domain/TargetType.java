package com.akandiah.propmanager.features.invite.domain;

/**
 * The type of entity that this invite is for.
 * Determines which table the target_id references.
 */
public enum TargetType {
	LEASE,
	PROPERTY,
	COMPANY,
	UNIT
}
