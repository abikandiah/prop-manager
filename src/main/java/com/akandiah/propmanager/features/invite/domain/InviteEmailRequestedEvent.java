package com.akandiah.propmanager.features.invite.domain;

import java.util.UUID;

/**
 * Published after an invite is committed to DB.
 * The listener sends the email and writes SENT/FAILED status in its own transaction.
 * Template context is loaded from {@code invite.getAttributes()} by the dispatcher;
 * computed fields (e.g. {@code inviteLink}) are added at dispatch time.
 *
 * @param inviteId ID of the persisted invite
 * @param isResend true when this is a resend (updates lastResentAt), false for initial send (updates sentAt)
 */
public record InviteEmailRequestedEvent(UUID inviteId, boolean isResend) {}
