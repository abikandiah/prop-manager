package com.akandiah.propmanager.security;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.akandiah.propmanager.config.AuditProperties;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Filter that logs an audit entry after each request: principal, method, URI,
 * status, and duration. Configuration is driven by {@code app.audit} in
 * application.yml. Uses the {@code AUDIT} logger so entries can be routed in
 * Logback (e.g. to a dedicated file or appender) via
 * {@code logging.logger.AUDIT}.
 */
@Slf4j(topic = "AUDIT")
public class AuditLoggingFilter extends OncePerRequestFilter {

	private final AuditProperties properties;

	public AuditLoggingFilter(AuditProperties properties) {
		this.properties = properties;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		if (!properties.enabled()) {
			filterChain.doFilter(request, response);
			return;
		}

		long startNanos = System.nanoTime();
		StatusCapturingResponseWrapper wrappedResponse = new StatusCapturingResponseWrapper(response);

		try {
			filterChain.doFilter(request, wrappedResponse);
		} finally {
			int status = wrappedResponse.getCapturedStatus();
			long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
			String principal = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
					.filter(Authentication::isAuthenticated)
					.map(Authentication::getName)
					.orElse("-");
			String method = request.getMethod();
			String uri = request.getRequestURI();
			String query = properties.includeQueryString() && request.getQueryString() != null
					? "?" + request.getQueryString()
					: "";
			String client = properties.includeClientInfo() ? request.getRemoteAddr() : "-";
			String timestamp = Instant.now().toString();

			log.info("timestamp={} principal={} method={} uri={}{} status={} durationMs={} client={}",
					timestamp, principal, method, uri, query, status, durationMs, client);
		}
	}
}
