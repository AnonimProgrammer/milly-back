package com.milly.common.idempotency;

import com.milly.common.exception.IdempotencyConflictException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

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
        String idempotencyKey = request == null ? null : request.getHeader(IDEMPOTENCY_KEY_HEADER);
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return joinPoint.proceed();
        }

        String scopeKey = buildScopeKey(request, idempotencyKey);
        String requestHash = hashRequestBody(joinPoint);

        Optional<IdempotencyRecord> existing = idempotencyStore.find(scopeKey);
        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();
            if (!record.requestHash().equals(requestHash)) {
                throw new IdempotencyConflictException();
            }
            return ResponseEntity.status(record.status()).body(record.body());
        }

        Object result = joinPoint.proceed();
        if (result instanceof ResponseEntity<?> responseEntity) {
            idempotencyStore.save(
                    scopeKey,
                    new IdempotencyRecord(requestHash, responseEntity.getStatusCode().value(), responseEntity.getBody()));
        }
        return result;
    }

    private String buildScopeKey(HttpServletRequest request, String idempotencyKey) {
        return request.getMethod() + ":" + request.getRequestURI() + ":" + resolveActor() + ":" + idempotencyKey;
    }

    private String resolveActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UUID userId) {
            return "user:" + userId;
        }
        return "anonymous";
    }

    private String hashRequestBody(ProceedingJoinPoint joinPoint) {
        Object body = extractRequestBody(joinPoint);
        try {
            String payload = body == null ? "" : objectMapper.writeValueAsString(body);
            return sha256(payload);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Unable to hash idempotent request body.", e);
        }
    }

    private Object extractRequestBody(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(RequestBody.class)) {
                return args[i];
            }
        }
        return null;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available.", e);
        }
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }
}
