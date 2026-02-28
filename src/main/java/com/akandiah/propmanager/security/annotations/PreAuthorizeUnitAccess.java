package com.akandiah.propmanager.security.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Meta-annotation for unit-level access checks.
 * Uses domain 'PORTFOLIO' and resource type 'UNIT'.
 * 
 * <p>Expects {@code #id} (Unit ID) and {@code #orgId} to be present in the SpEL context.
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("@permissionGuard.hasAccess('{value}', 'PORTFOLIO', 'UNIT', #id, #orgId)")
public @interface PreAuthorizeUnitAccess {
	String value() default "READ";
}
