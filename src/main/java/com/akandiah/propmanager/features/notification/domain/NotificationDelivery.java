package com.akandiah.propmanager.features.notification.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import com.akandiah.propmanager.common.notification.NotificationChannel;
import com.akandiah.propmanager.common.notification.NotificationReferenceType;
import com.akandiah.propmanager.common.notification.NotificationType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notification_deliveries")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NotificationDelivery {

	@Id
	@UuidGenerator(style = UuidGenerator.Style.TIME)
	private UUID id;

	@Version
	@Column(nullable = false)
	private Integer version;

	@Column(name = "user_id")
	private UUID userId;

	@Column(name = "recipient_address", nullable = false, length = 320)
	private String recipientAddress;

	@Enumerated(EnumType.STRING)
	@Column(name = "notification_type", nullable = false, length = 64)
	private NotificationType notificationType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private NotificationChannel channel;

	@Column(name = "reference_id")
	private UUID referenceId;

	@Enumerated(EnumType.STRING)
	@Column(name = "reference_type", length = 64)
	private NotificationReferenceType referenceType;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "template_context")
	private Map<String, Object> templateContext;

	@Setter
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	@Builder.Default
	private NotificationDeliveryStatus status = NotificationDeliveryStatus.PENDING;

	@Setter
	@Column(name = "retry_count", nullable = false)
	@Builder.Default
	private int retryCount = 0;

	@Setter
	@Column(name = "error_message", length = 500)
	private String errorMessage;

	@Setter
	@Column(name = "sent_at")
	private Instant sentAt;

	@Setter
	@Column(name = "viewed_at")
	private Instant viewedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@PrePersist
	void prePersist() {
		Instant now = Instant.now();
		createdAt = updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = Instant.now();
	}
}
