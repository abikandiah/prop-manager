package com.akandiah.propmanager.features.notification.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.akandiah.propmanager.common.notification.NotificationReferenceType;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, UUID> {

	List<NotificationDelivery> findByUserIdOrderByCreatedAtDesc(UUID userId);

	/**
	 * Finds the most recent delivery for a given reference (e.g. the latest email attempt for an Invite).
	 */
	java.util.Optional<NotificationDelivery> findTopByReferenceTypeAndReferenceIdOrderByCreatedAtDesc(
			NotificationReferenceType referenceType, UUID referenceId);

	/**
	 * Finds the most recent delivery per reference ID for a batch of references.
	 * Used to populate delivery status on LeaseTenantResponse without N+1 queries.
	 */
	@Query("""
			SELECT d FROM NotificationDelivery d
			WHERE d.referenceType = :referenceType
			AND d.referenceId IN :referenceIds
			AND d.createdAt = (
			    SELECT MAX(d2.createdAt) FROM NotificationDelivery d2
			    WHERE d2.referenceType = d.referenceType
			    AND d2.referenceId = d.referenceId
			)
			""")
	List<NotificationDelivery> findLatestByReferenceTypeAndReferenceIdIn(
			@Param("referenceType") NotificationReferenceType referenceType,
			@Param("referenceIds") java.util.Collection<UUID> referenceIds);

	@Query("""
			SELECT d FROM NotificationDelivery d
			WHERE d.status = 'FAILED'
			AND d.retryCount < :maxRetries
			AND d.updatedAt < :retryBefore
			ORDER BY d.updatedAt ASC
			""")
	List<NotificationDelivery> findRetryableFailedDeliveries(
			@Param("maxRetries") int maxRetries,
			@Param("retryBefore") Instant retryBefore);

	/**
	 * Finds PENDING deliveries that have been stuck longer than the given threshold.
	 * Used by the retry scheduler to recover deliveries whose async send was lost (e.g. JVM crash).
	 */
	@Query("""
			SELECT d FROM NotificationDelivery d
			WHERE d.status = 'PENDING'
			AND d.createdAt < :stuckBefore
			ORDER BY d.createdAt ASC
			""")
	List<NotificationDelivery> findStuckPendingDeliveries(@Param("stuckBefore") Instant stuckBefore);

	/**
	 * Bulk-cancels PENDING and FAILED deliveries for a given reference (e.g. before a resend).
	 */
	@Modifying
	@Query("""
			UPDATE NotificationDelivery d
			SET d.status = 'CANCELLED'
			WHERE d.referenceType = :referenceType
			AND d.referenceId = :referenceId
			AND d.status IN ('PENDING', 'FAILED')
			""")
	int cancelActiveDeliveriesForReference(
			@Param("referenceType") NotificationReferenceType referenceType,
			@Param("referenceId") UUID referenceId);
}
