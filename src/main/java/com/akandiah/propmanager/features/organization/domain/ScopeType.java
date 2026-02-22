package com.akandiah.propmanager.features.organization.domain;

/**
 * Type of scope for granular access. Used on MemberScope.
 * When present, the membership applies only to these resources within the org.
 */
public enum ScopeType {
	PROPERTY,
	UNIT
}
