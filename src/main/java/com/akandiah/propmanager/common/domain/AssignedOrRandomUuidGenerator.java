package com.akandiah.propmanager.common.domain;

import java.util.EnumSet;
import java.util.UUID;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.id.uuid.UuidVersion7Strategy;

/**
 * Hibernate ID generator that honours a client-supplied UUID when present,
 * falling back to a server-generated UUID v7 when the field is null.
 *
 * <p>This enables the offline-first pattern where the frontend pre-generates a
 * stable UUID v7 for every new entity. The same ID is used for the optimistic
 * cache entry and persisted to the database, so no ID swap occurs after the
 * server confirms the write.
 *
 * <p>Usage: annotate the {@code @Id} field with
 * {@code @IdGeneratorType(AssignedOrRandomUuidGenerator.class)}.
 */
public class AssignedOrRandomUuidGenerator implements BeforeExecutionGenerator {

@Override
    public Object generate(SharedSessionContractImplementor session, Object owner, Object currentValue, EventType eventType) {
        // In Hibernate 6, 'currentValue' is the ID already on the entity
        if (currentValue instanceof UUID uuid) {
            return uuid;
        }
        // Fallback to generating a new one
        return UuidVersion7Strategy.INSTANCE.generateUuid(session);
    }

    @Override
    public EnumSet<EventType> getEventTypes() {
        return EnumSet.of(EventType.INSERT);
    }

    @Override
    public boolean allowAssignedIdentifiers() {
        return true; 
    }
}
