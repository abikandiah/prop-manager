package com.akandiah.propmanager.features.membership.domain;

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

	Optional<Membership> findByIdAndOrganizationId(UUID id, UUID organizationId);

	@Query("SELECT m FROM Membership m JOIN FETCH m.organization WHERE m.user.id = :userId")
	List<Membership> findByUserIdWithOrganization(UUID userId);

	@Query("SELECT m FROM Membership m LEFT JOIN FETCH m.user JOIN FETCH m.organization LEFT JOIN FETCH m.invite WHERE m.organization.id = :organizationId")
	List<Membership> findByOrganizationIdWithUserAndOrg(UUID organizationId);

	@Query("SELECT m FROM Membership m JOIN FETCH m.user JOIN FETCH m.organization WHERE m.user.id = :userId")
	List<Membership> findByUserIdWithUserAndOrg(UUID userId);

	@Query("""
			SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END
			FROM Membership m JOIN m.invite i
			WHERE m.organization.id = :organizationId
			  AND m.user IS NULL
			  AND i.email = :email
			  AND i.status = com.akandiah.propmanager.features.invite.domain.InviteStatus.PENDING
			""")
	boolean existsPendingInviteForEmailInOrg(String email, UUID organizationId);
}
