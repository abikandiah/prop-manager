package com.akandiah.propmanager.security.dev.dto;

/**
 * Response for POST /dev/token. Contains the JWT for use as Bearer token.
 */
public record DevTokenResponse(String access_token) {
}
