package com.akandiah.propmanager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

import com.akandiah.propmanager.security.RateLimitFilter;

/**
 * Stateless JWT-based security using Spring Security OAuth2 Resource Server
 * (built-in).
 * All /api/** (except explicitly permitted) require a valid JWT.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

	private final RateLimitFilter rateLimitFilter;
	private final JwtAuthenticationConverter jwtAuthenticationConverter;

	private final CorsConfigurationSource corsConfigurationSource;
	private final Environment environment;

	public SecurityConfig(RateLimitFilter rateLimitFilter, JwtAuthenticationConverter jwtAuthenticationConverter,
			CorsConfigurationSource corsConfigurationSource, Environment environment) {
		this.rateLimitFilter = rateLimitFilter;
		this.jwtAuthenticationConverter = jwtAuthenticationConverter;
		this.corsConfigurationSource = corsConfigurationSource;
		this.environment = environment;
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		boolean h2ConsoleEnabled = environment.getProperty("spring.h2.console.enabled", Boolean.class, false);

		http
				.cors(cors -> cors.configurationSource(corsConfigurationSource))
				// CSRF disabled for stateless JWT API
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session
						.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> {
					// 1. Infrastructure & Health
					auth.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
							.requestMatchers("/error").permitAll();

					// 2. Swagger / OpenAPI (Standard paths)
					auth.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();

					// 3. Public API
					auth.requestMatchers("/api/public/**").permitAll();

					// 4. Dev Utilities (Login for JWT generation - controller is @Profile("dev"))
					auth.requestMatchers("/api/dev/**").permitAll();

					// 5. H2 Console (Dev only)
					if (h2ConsoleEnabled) {
						auth.requestMatchers("/h2-console/**").permitAll();
					}

					// 6. Secure Everything Else
					auth.requestMatchers("/api/**").authenticated();

					// 7. Deny all other requests (fallback)
					auth.anyRequest().denyAll();
				})
				.headers(headers -> {
					// Security Headers
					headers
							.contentSecurityPolicy(csp -> csp
									.policyDirectives("default-src 'self'; frame-ancestors 'self'; form-action 'self'"))
							.httpStrictTransportSecurity(hsts -> hsts
									.includeSubDomains(true)
									.maxAgeInSeconds(31536000)) // 1 year
							.xssProtection(xss -> xss.disable()) // Disable deprecated XSS header (CSP is preferred)
							.frameOptions(frame -> {
								if (h2ConsoleEnabled) {
									frame.sameOrigin();
								} else {
									frame.deny();
								}
							});
				})
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))

				// Place Rate Limiting at the very front of the line
				.addFilterBefore(rateLimitFilter, BearerTokenAuthenticationFilter.class);

		return http.build();
	}
}
