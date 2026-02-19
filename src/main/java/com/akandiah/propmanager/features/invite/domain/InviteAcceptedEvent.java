package com.akandiah.propmanager.features.invite.domain;

import com.akandiah.propmanager.features.user.domain.User;

/**
 * Published after an invite is successfully redeemed.
 * Handlers react based on {@link Invite#getTargetType()} to perform
 * target-specific post-acceptance logic (e.g. linking a LeaseTenant).
 */
public record InviteAcceptedEvent(Invite invite, User claimedUser) {
}
