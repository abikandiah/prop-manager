package com.akandiah.propmanager.features.activity.domain;

import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.akandiah.propmanager.common.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** Immutable audit record for a domain action within an org. Append-only — never updated. */
@Entity
@Table(name = "activity_events")
@Getter
@SuperBuilder
@NoArgsConstructor
public class ActivityEvent extends BaseEntity {

	@Column(name = "org_id", nullable = false)
	private UUID orgId;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false, length = 64)
	private ActivityEventType eventType;

	@Enumerated(EnumType.STRING)
	@Column(name = "subject_type", nullable = false, length = 32)
	private ActivitySubjectType subjectType;

	@Column(name = "subject_id")
	private UUID subjectId;

	@Column(name = "actor_id")
	private UUID actorId;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "metadata")
	private Map<String, Object> metadata;

}
