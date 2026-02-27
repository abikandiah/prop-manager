package com.akandiah.propmanager.common.domain;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Persistable;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Base class for all user-facing domain entities.
 *
 * <p>Provides:
 * <ul>
 *   <li>UUID v7 ID generation that honours a client-supplied ID when present
 *       ({@link AssignedOrRandomUuidGenerator}).</li>
 *   <li>{@link Persistable} implementation with an {@code isNew} flag so Spring
 *       Data JPA calls {@code persist()} (INSERT) rather than {@code merge()}
 *       (UPDATE) for new entities that carry a client-supplied ID.</li>
 *   <li>Optimistic locking via {@code @Version}.</li>
 *   <li>Audit timestamps ({@code createdAt}, {@code updatedAt}) populated by
 *       JPA lifecycle hooks.</li>
 * </ul>
 *
 * <p><strong>Subclass requirements:</strong>
 * <ul>
 *   <li>Replace {@code @Builder} with {@code @SuperBuilder} — {@code @Builder}
 *       does not see inherited fields, so {@code .id(...)} would silently
 *       disappear from the builder.</li>
 *   <li>Remove {@code @AllArgsConstructor} — {@code @SuperBuilder} generates
 *       its own constructor chain; coexistence causes a compile-time conflict.</li>
 *   <li>Keep {@code @NoArgsConstructor} — JPA requires a no-args constructor to
 *       instantiate proxies.</li>
 *   <li>Remove the now-redundant {@code @Id}, {@code @UuidGenerator},
 *       {@code @Version}, {@code createdAt}, {@code updatedAt},
 *       {@code @PrePersist}, and {@code @PreUpdate} declarations.</li>
 * </ul>
 */
@MappedSuperclass
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseEntity implements Persistable<UUID> {

    @Id
    @AssignedOrRandomUuid
    private UUID id;

    @Version
    @Column(nullable = false)
    private Integer version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Tracks whether this instance is a new entity (not yet persisted).
     * Starts {@code true}; cleared by {@code @PostPersist} and {@code @PostLoad}
     * so Spring Data JPA calls {@code persist()} (INSERT) rather than
     * {@code merge()} (UPDATE) for new entities with a client-supplied ID.
     */
    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    @JsonIgnore
    public boolean isNew() {
        return isNew;
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
		if (this.createdAt == null) {
			this.createdAt = now;
		}
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
