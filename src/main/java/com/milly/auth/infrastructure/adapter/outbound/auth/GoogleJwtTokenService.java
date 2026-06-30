package com.milly.auth.infrastructure.adapter.outbound.auth;

import com.milly.auth.infrastructure.config.AuthProperties;
import com.milly.common.exception.InvalidCredentialsException;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Objects;

@Service
public class GoogleJwtTokenService {

    private static final String GOOGLE_ISSUER = "https://accounts.google.com";

    private final String googleClientId;
    private final NimbusJwtDecoder jwtDecoder;

    public GoogleJwtTokenService(AuthProperties authProperties) {
        this.googleClientId = authProperties.google().clientId();
        this.jwtDecoder = NimbusJwtDecoder.withIssuerLocation(GOOGLE_ISSUER).build();
        this.jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(), new JwtIssuerValidator(GOOGLE_ISSUER), audienceValidator()));
    }

    public Jwt decodeIdentityToken(String idToken) {
        if (googleClientId.isBlank()) {
            throw new InvalidCredentialsException("Google authentication is not configured.");
        }

        try {
            return jwtDecoder.decode(idToken);
        } catch (JwtException exception) {
            throw new InvalidCredentialsException("Invalid Google identity token.");
        }
    }

    public boolean isEmailVerified(Jwt jwt) {
        Object emailVerifiedClaim = jwt.getClaims().get("email_verified");
        if (emailVerifiedClaim instanceof Boolean bool) {
            return bool;
        }
        if (emailVerifiedClaim instanceof String text) {
            return Boolean.parseBoolean(text);
        }
        return false;
    }

    private OAuth2TokenValidator<Jwt> audienceValidator() {
        OAuth2Error error = new OAuth2Error("invalid_token", "Google identity token has invalid audience.", null);
        return jwt -> isAudienceValid(jwt)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(error);
    }

    private boolean isAudienceValid(Jwt jwt) {
        if (googleClientId.isBlank()) {
            return false;
        }

        Object audClaim = jwt.getClaims().get("aud");
        if (audClaim instanceof String audience) {
            return googleClientId.equals(audience);
        }
        if (audClaim instanceof Collection<?> audiences) {
            return audiences.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .anyMatch(googleClientId::equals);
        }
        return false;
    }
}
