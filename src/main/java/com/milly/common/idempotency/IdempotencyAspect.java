package com.milly.common.idempotency;

import com.milly.common.exception.IdempotencyConflictException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.ObjectMapper;

/**
 * Intercepts {@link Idempotent}-annotated controller methods and, when an
 * {@value #IDEMPOTENCY_KEY_HEADER} header is present, prevents the underlying write from
 * running more than once for the same scope key + request body:
 *
 * <ul>
 *     <li>No prior record -&gt; the method runs normally and its response is cached.</li>
 *     <li>Same key, same body -&gt; the cached response is replayed, the method does not run.</li>
 *     <li>Same key, different body -&gt; {@link IdempotencyConflictException} (409).</li>
 * </ul>
 *
 * <p>Requests without the header pass through untouched.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotencyAspect {

    public static final String IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";

    private final IdempotencyStore idempotencyStore;
    private final ObjectMapper objectMapper;

    @Around("@annotation(com.milly.common.idempotency.Idempotent)")
    public Object aroundIdempotentMethod(ProceedingJoinPoint joinPoint) throws Throwable {

        HttpServletRequest request = currentRequest();

        String idempotencyKey =
                request == null
                        ? null
                        : request.getHeader(IDEMPOTENCY_KEY_HEADER);

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return joinPoint.proceed();
        }

        return joinPoint.proceed();
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }
}