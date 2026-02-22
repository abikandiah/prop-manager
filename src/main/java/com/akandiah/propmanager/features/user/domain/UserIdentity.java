package com.akandiah.propmanager.features.user.domain;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Links an OIDC identity (issuer + subject) to a single user account.
 * One user can have multiple identities (e.g. Google and Meta with the same email).
 */
@Entity
@Table(name = "user_identities", uniqueConstraints = {
		@UniqueConstraint(name = "uk_user_identities_issuer_sub", columnNames = { "issuer", "sub" })
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserIdentity {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.TIME)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, length = 512)
	private String issuer;

	@Column(nullable = false, length = 255)
	private String sub;

	@Column(name = "created_at", nullable = false, updatable = false)
	@Setter(AccessLevel.NONE)
	private Instant createdAt;

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}
}
