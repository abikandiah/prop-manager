package com.akandiah.propmanager.features.user.service;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.akandiah.propmanager.features.user.domain.User;
import com.akandiah.propmanager.features.user.domain.UserRepository;
import com.akandiah.propmanager.features.user.domain.UserRole;
import com.akandiah.propmanager.features.user.domain.UserStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public Optional<User> findByEmail(String email) {
		return userRepository.findByEmail(email);
	}

	@Transactional(readOnly = true)
	public Optional<User> findByProviderId(String providerId) {
		return userRepository.findByProviderId(providerId);
	}

	@Transactional
	public Optional<User> syncUserIfExists(String providerId, String name, String email) {
		return userRepository.findByProviderId(providerId)
				.or(() -> userRepository.findByEmail(email))
				.map(user -> {
					log.info("Updating existing user: {}", email);
					if (name != null)
						user.setName(name);
					if (email != null)
						user.setEmail(email);
					user.setProviderId(providerId);
					return userRepository.save(user);
				});
	}

	@Transactional
	public User saveUser(User user) {
		log.info("Saving user: {}", user.getEmail());
		return userRepository.save(user);
	}

	@Transactional
	public User registerUser(String providerId, String name, String email) {
		if (userRepository.findByProviderId(providerId).isPresent()) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST, "User already exists with this provider ID");
		}
		if (email != null && userRepository.findByEmail(email).isPresent()) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST, "User already exists with this email");
		}

		log.info("Registering new user: {}", email);
		User newUser = User.builder()
				.providerId(providerId)
				.name(name != null ? name : "User " + providerId)
				.email(email)
				.role(UserRole.TENANT)
				.status(UserStatus.ACTIVE)
				.build();
		return userRepository.save(newUser);
	}
}
