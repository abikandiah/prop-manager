package com.akandiah.propmanager.features.user.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.akandiah.propmanager.features.user.domain.User;
import com.akandiah.propmanager.features.user.domain.UserRepository;

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
	public Optional<User> findByIdpSub(String idpSub) {
		return userRepository.findByIdpSub(idpSub);
	}

	@Transactional
	public Optional<User> syncUserIfExists(String idpSub, String name, String email) {
		return userRepository.findByIdpSub(idpSub)
				.map(user -> {
					log.info("Updating existing user: {}", email);
					if (name != null)
						user.setName(name);
					if (email != null)
						user.setEmail(email);
					return userRepository.save(user);
				});
	}

	@Transactional
	public User saveUser(User user) {
		log.info("Saving user: {}", user.getEmail());
		return userRepository.save(user);
	}

	@Transactional
	public User registerUser(String idpSub, String name, String email) {
		return syncUserIfExists(idpSub, name, email)
				.orElseGet(() -> {
					log.info("Registering new user: {}", email);
					User newUser = User.builder()
							.idpSub(idpSub)
							.name(name != null ? name : "User " + idpSub)
							.email(email)
							.build();
					return userRepository.save(newUser);
				});
	}
}
