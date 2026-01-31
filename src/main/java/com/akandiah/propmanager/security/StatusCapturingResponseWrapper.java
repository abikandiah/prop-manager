package com.akandiah.propmanager.security;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

/**
 * Wraps the response to capture the HTTP status code for audit logging.
 */
final class StatusCapturingResponseWrapper extends HttpServletResponseWrapper {

	private int status = HttpServletResponse.SC_OK;

	StatusCapturingResponseWrapper(HttpServletResponse response) {
		super(response);
	}

	@Override
	public void setStatus(int sc) {
		super.setStatus(sc);
		this.status = sc;
	}

	@Override
	public void sendError(int sc) {
		try {
			super.sendError(sc);
			this.status = sc;
		} catch (java.io.IOException ignored) {
			this.status = sc;
		}
	}

	@Override
	public void sendError(int sc, String msg) {
		try {
			super.sendError(sc, msg);
			this.status = sc;
		} catch (java.io.IOException ignored) {
			this.status = sc;
		}
	}

	int getCapturedStatus() {
		return status;
	}
}
