package com.akandiah.propmanager.features.invite.domain;

/**
 * Status of an invitation throughout its lifecycle.
 */
public enum InviteStatus {
	/** Invite created but not yet accepted */
	PENDING,

	/** Invite accepted and user account created/linked */
	ACCEPTED,

	/** Invite has passed its expiration date */
	EXPIRED,

	/** Invite was manually revoked before acceptance */
	REVOKED
}
