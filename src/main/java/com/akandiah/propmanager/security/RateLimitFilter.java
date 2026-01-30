package com.akandiah.propmanager.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.akandiah.propmanager.config.RateLimitProperties;
import com.github.benmanes.caffeine.cache.Cache;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Rate limiter per client IP using Resilience4j (Spring ecosystem standard).
 * Returns 429 Too Many Requests when limit exceeded.
 */
@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

	private final RateLimiterRegistry registry;
	private final Cache<String, RateLimiter> cache;
	private final RateLimitProperties props;

	public RateLimitFilter(
			RateLimiterRegistry registry,
			Cache<String, RateLimiter> cache,
			RateLimitProperties props) {
		this.registry = registry;
		this.cache = cache;
		this.props = props;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {

		// Use the properties object for a cleaner check
		if (!props.enabled() || isStaticResource(request)) {
			filterChain.doFilter(request, response);
			return;
		}

		String clientKey = resolveClientKey(request);

		// The cache handles the "memoization" of the RateLimiter
		RateLimiter rateLimiter = cache.get(clientKey, this::createRateLimiter);

		if (rateLimiter.acquirePermission()) {
			filterChain.doFilter(request, response);
		} else {
			handleLimitExceeded(response, clientKey, rateLimiter);
		}
	}

	private RateLimiter createRateLimiter(String clientKey) {
		// Look for the "default" config defined in application.yml
		RateLimiterConfig config = registry.getConfiguration("default")
				.orElse(RateLimiterConfig.ofDefaults());

		return registry.rateLimiter("api-" + clientKey, config);
	}

	private void handleLimitExceeded(HttpServletResponse response, String key, RateLimiter rateLimiter)
			throws IOException {
		long retryAfter = rateLimiter.getRateLimiterConfig().getLimitRefreshPeriod().toSeconds();

		response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
		response.setHeader("Retry-After", String.valueOf(retryAfter));
		response.setContentType("application/json");

		response.getWriter().write(String.format("""
				{
				    "error": "Too Many Requests",
				    "message": "Quota exceeded for IP %s",
				    "retry_after_seconds": %d
				}
				""", key, retryAfter));
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