package com.akandiah.propmanager.security.dev;

import java.util.Date;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.akandiah.propmanager.config.JwtProperties;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Configuration
@Profile("dev")
public class DevTokenGenerator {

	@Bean
	CommandLineRunner printDevToken(JwtProperties props) {
		return args -> {
			String token = Jwts.builder()
					.header()
					.add("typ", "JWT")
					.and()
					.subject("dev-admin")
					.issuer(props.issuer())
					.issuedAt(new Date())
					.expiration(new Date(System.currentTimeMillis() + props.expirationMs()))
					.claim("groups", List.of("ADMIN"))
					// Algorithm is auto-detected based on the key
					.signWith(Keys.hmacShaKeyFor(props.secret().getBytes()))
					.compact();

			System.out.println("\n--- DEV MODE ADMIN TOKEN ---");
			System.out.println("Bearer " + token);
			System.out.println("-----------------------------\n");
		};
	}
}