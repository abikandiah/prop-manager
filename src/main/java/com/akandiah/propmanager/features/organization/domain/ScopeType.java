package com.akandiah.propmanager.features.organization.domain;

import com.akandiah.propmanager.common.permission.ResourceType;

/**
 * Type of scope for granular access. Used on MemberScope.
 * ORG = org-wide access; PROPERTY = one property; UNIT = one unit.
 */
public enum ScopeType {
	ORG {
		@Override
		public ResourceType toResourceType() {
			return ResourceType.ORG;
		}
	},
	PROPERTY {
		@Override
		public ResourceType toResourceType() {
			return ResourceType.PROPERTY;
		}
	},
	UNIT {
		@Override
		public ResourceType toResourceType() {
			return ResourceType.UNIT;
		}
	};

	public abstract ResourceType toResourceType();
}
