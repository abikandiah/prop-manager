package com.akandiah.propmanager.features.invite.domain;

import java.util.Map;
import java.util.UUID;

/**
 * Published after an invite is committed to DB.
 * The listener sends the email and writes SENT/FAILED status in its own transaction.
 *
 * @param inviteId ID of the persisted invite
 * @param isResend true when this is a resend (updates lastResentAt), false for initial send (updates sentAt)
 * @param metadata Additional template variables supplied by the caller (e.g. property name)
 */
public record InviteEmailRequestedEvent(UUID inviteId, boolean isResend, Map<String, Object> metadata) {}
