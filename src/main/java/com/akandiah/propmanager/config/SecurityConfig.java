package com.akandiah.propmanager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
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

	public SecurityConfig(RateLimitFilter rateLimitFilter, JwtAuthenticationConverter jwtAuthenticationConverter,
			CorsConfigurationSource corsConfigurationSource) {
		this.rateLimitFilter = rateLimitFilter;
		this.jwtAuthenticationConverter = jwtAuthenticationConverter;
		this.corsConfigurationSource = corsConfigurationSource;
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.cors(cors -> cors.configurationSource(corsConfigurationSource))
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session
						.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						// 1. Infrastructure & Health
						.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
						.requestMatchers("/error").permitAll()

						// 2. Swagger / OpenAPI (Standard paths)
						.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

						// 3. Public API
						.requestMatchers("/api/public/**").permitAll()

						// 4. Secure Everything Else
						.requestMatchers("/api/**").authenticated()
						.anyRequest().authenticated())

				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))

				// Place Rate Limiting at the very front of the line
				.addFilterBefore(rateLimitFilter, BearerTokenAuthenticationFilter.class);

		return http.build();
	}
}
