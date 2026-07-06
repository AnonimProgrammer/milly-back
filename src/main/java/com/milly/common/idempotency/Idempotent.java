package com.milly.common.idempotency;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a write endpoint as safe to retry: repeating the same request with the same
 * {@code X-Idempotency-Key} header and body returns the original cached response instead of
 * performing the side effect again. Reusing the key with a different body results in a 409.
 *
 * <p>Requests without the header are not intercepted and behave as if this annotation were absent.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
}
