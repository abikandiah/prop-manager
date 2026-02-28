package com.akandiah.propmanager.security.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Meta-annotation for lease-level access checks.
 * Uses domain 'LEASES' and resolves the unit via the lease ID.
 *
 * <p>Expects {@code #id} (Lease ID) and {@code #orgId} to be present in the SpEL context.
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("@permissionGuard.hasLeaseAccess('{value}', 'LEASES', #id, #orgId)")
public @interface PreAuthorizeLeaseAccess {
	String value() default "READ";
}
