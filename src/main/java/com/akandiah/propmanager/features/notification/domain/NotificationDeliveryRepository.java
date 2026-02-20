package com.akandiah.propmanager.features.notification.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, UUID> {

	List<NotificationDelivery> findByUserIdOrderByCreatedAtDesc(UUID userId);

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
}
