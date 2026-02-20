package com.akandiah.propmanager.features.invite.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository for Invite entities.
 */
public interface InviteRepository extends JpaRepository<Invite, UUID> {

	/**
	 * Find an invite by its unique token.
	 */
	Optional<Invite> findByToken(String token);

	/**
	 * Find all invites for a specific target (e.g., all invites for a specific lease).
	 */
	List<Invite> findByTargetTypeAndTargetId(TargetType targetType, UUID targetId);

	/**
	 * Find all invites for a specific email address.
	 */
	List<Invite> findByEmail(String email);

	/**
	 * Find all invites with a specific status.
	 */
	List<Invite> findByStatus(InviteStatus status);

	/**
	 * Find all pending invites that have expired.
	 */
	@Query("SELECT i FROM Invite i WHERE i.status = 'PENDING' AND i.expiresAt < :now")
	List<Invite> findExpiredPendingInvites(@Param("now") Instant now);

	/**
	 * Find a pending invite by email and target.
	 * Useful to check if a user already has a pending invite for a resource.
	 */
	Optional<Invite> findByEmailAndTargetTypeAndTargetIdAndStatus(String email, TargetType targetType, UUID targetId,
			InviteStatus status);

	/**
	 * Check if a pending invite exists for a given email and target.
	 */
	boolean existsByEmailAndTargetTypeAndTargetIdAndStatus(String email, TargetType targetType, UUID targetId,
			InviteStatus status);

	/**
	 * Fetch invite with its invitedBy user eagerly loaded.
	 * Used by the NotificationDispatcher on an async thread where no Hibernate session is active.
	 */
	@Query("SELECT i FROM Invite i WHERE i.id = :id")
	@EntityGraph(attributePaths = {"invitedBy"})
	Optional<Invite> findWithInvitedByById(@Param("id") UUID id);
}
