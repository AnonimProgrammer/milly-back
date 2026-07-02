package com.milly.auth.infrastructure.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
class JwtSecretValidator {

    private final AuthProperties authProperties;
    private final Environment environment;

    @PostConstruct
    void validateProductionSecret() {
        if (!Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            return;
        }

        String secret = authProperties.jwt().secret();
        if (secret == null
                || secret.isBlank()
                || secret.equals(AuthProperties.Jwt.DEV_FALLBACK_SECRET)) {
            throw new IllegalStateException(
                    "JWT_SECRET must be set to a strong random value in production.");
        }
    }
}
