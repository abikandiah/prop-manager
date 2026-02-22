package com.akandiah.propmanager.features.user.domain;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserIdentityRepository extends JpaRepository<UserIdentity, UUID> {

	Optional<UserIdentity> findByIssuerAndSub(String issuer, String sub);

	@Query("select ui from UserIdentity ui join fetch ui.user where ui.issuer = :issuer and ui.sub = :sub")
	Optional<UserIdentity> findByIssuerAndSubWithUser(String issuer, String sub);
}
