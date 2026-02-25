package com.akandiah.propmanager.security;

import java.io.IOException;
import java.net.URI;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.akandiah.propmanager.config.RateLimitProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Per-IP rate limiter using Resilience4j. Returns 429 when limit exceeded. */
@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

	private final RateLimiterRegistry registry;
	@Qualifier("rateLimitCache")
	private final Cache<String, RateLimiter> cache;
	private final RateLimitProperties props;
	private final ObjectMapper objectMapper;

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {

		if (!props.enabled() || isStaticResource(request)) {
			filterChain.doFilter(request, response);
			return;
		}

		String clientKey = resolveClientKey(request);

		RateLimiter rateLimiter = cache.get(clientKey, this::createRateLimiter);

		if (rateLimiter.acquirePermission()) {
			filterChain.doFilter(request, response);
		} else {
			handleLimitExceeded(response, rateLimiter);
		}
	}

	private RateLimiter createRateLimiter(String clientKey) {
		RateLimiterConfig config = registry.getConfiguration("default")
				.orElse(RateLimiterConfig.ofDefaults());

		return registry.rateLimiter("api-" + clientKey, config);
	}

	private void handleLimitExceeded(HttpServletResponse response, RateLimiter rateLimiter)
			throws IOException {
		long retryAfter = rateLimiter.getRateLimiterConfig().getLimitRefreshPeriod().toSeconds();

		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
				HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded");
		problem.setTitle("Too Many Requests");
		problem.setType(URI.create("about:blank"));
		problem.setProperty("retryAfterSeconds", retryAfter);

		response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
		response.setHeader("Retry-After", String.valueOf(retryAfter));
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), problem);
	}

	private String resolveClientKey(HttpServletRequest request) {
		String xff = request.getHeader("X-Forwarded-For");
		if (xff != null && !xff.isBlank()) {
			return xff.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

	private boolean isStaticResource(HttpServletRequest request) {
		String path = request.getRequestURI();
		return path.startsWith("/swagger-ui") ||
				path.startsWith("/v3/api-docs") ||
				path.startsWith("/favicon.ico");
	}
}
