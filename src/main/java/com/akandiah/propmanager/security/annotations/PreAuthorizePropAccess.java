package com.akandiah.propmanager.security.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Meta-annotation for property-level access checks.
 * Uses domain 'PORTFOLIO' and resource type 'PROPERTY'.
 * 
 * <p>The annotation {@code value} is the CRUD action: {@code "READ"}, {@code "CREATE"},
 * {@code "UPDATE"}, or {@code "DELETE"}.
 * Expects {@code #id} (Property ID) and {@code #orgId} to be present in the SpEL context.
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("@permissionGuard.hasAccess('{value}', 'PORTFOLIO', 'PROPERTY', #id, #orgId)")
public @interface PreAuthorizePropAccess {
	String value() default "READ";
}
