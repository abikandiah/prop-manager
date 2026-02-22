package com.akandiah.propmanager.features.organization.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

	List<Membership> findByUserId(UUID userId);

	List<Membership> findByOrganizationId(UUID organizationId);

	long countByOrganizationId(UUID organizationId);

	Optional<Membership> findByUserIdAndOrganizationId(UUID userId, UUID organizationId);

	boolean existsByUserIdAndOrganizationId(UUID userId, UUID organizationId);

	@Query("SELECT m FROM Membership m JOIN FETCH m.organization WHERE m.user.id = :userId")
	List<Membership> findByUserIdWithOrganization(UUID userId);

	@Query("SELECT m FROM Membership m JOIN FETCH m.user JOIN FETCH m.organization WHERE m.organization.id = :organizationId")
	List<Membership> findByOrganizationIdWithUserAndOrg(UUID organizationId);

	@Query("SELECT m FROM Membership m JOIN FETCH m.user JOIN FETCH m.organization WHERE m.user.id = :userId")
	List<Membership> findByUserIdWithUserAndOrg(UUID userId);
}
