package com.akandiah.propmanager.security;

import java.io.IOException;
import java.net.URI;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

/**
 * Catches AuthenticationServiceException (e.g. JWKS endpoint unreachable or returning an error)
 * before it propagates to Tomcat and becomes a 500. Returns 503 with ProblemDetail JSON.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
@RequiredArgsConstructor
public class JwksUnavailableFilter extends OncePerRequestFilter {

	private final ObjectMapper objectMapper;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		try {
			filterChain.doFilter(request, response);
		} catch (AuthenticationServiceException ex) {
			log.warn("Auth service unavailable at {}", request.getRequestURI(), ex);
			ProblemDetail pd = ProblemDetail.forStatusAndDetail(
					HttpStatus.SERVICE_UNAVAILABLE,
					"The authentication service is temporarily unavailable. Please try again later.");
			pd.setTitle("Auth Service Unavailable");
			pd.setInstance(URI.create(request.getRequestURI()));
			FilterProblemResponse.write(response, pd, objectMapper);
		}
	}
}
