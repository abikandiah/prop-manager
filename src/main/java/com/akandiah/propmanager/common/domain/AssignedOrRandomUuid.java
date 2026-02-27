package com.akandiah.propmanager.common.domain;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.annotations.IdGeneratorType;

/**
 * Marks a UUID {@code @Id} field to use {@link AssignedOrRandomUuidGenerator}.
 *
 * <p>Honous a client-supplied UUID if the field is non-null at INSERT time;
 * falls back to a server-generated UUID v7 otherwise.
 *
 * <p>Usage:
 * <pre>{@code
 * @Id
 * @AssignedOrRandomUuid
 * private UUID id;
 * }</pre>
 */
@IdGeneratorType(AssignedOrRandomUuidGenerator.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface AssignedOrRandomUuid {}
