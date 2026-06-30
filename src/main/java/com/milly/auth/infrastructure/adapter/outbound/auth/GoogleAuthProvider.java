package com.milly.auth.infrastructure.adapter.outbound.auth;

import com.milly.auth.application.model.ExternalIdentity;
import com.milly.auth.application.port.outbound.AuthProvider;
import com.milly.auth.domain.Credentials;
import com.milly.auth.domain.valueobject.AuthProviderType;
import com.milly.common.exception.InvalidCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GoogleAuthProvider implements AuthProvider {

    private final GoogleJwtTokenService googleJwtTokenService;

    public GoogleAuthProvider(GoogleJwtTokenService googleJwtTokenService) {
        this.googleJwtTokenService = googleJwtTokenService;
    }

    @Override
    public AuthProviderType getType() {
        return AuthProviderType.GOOGLE;
    }

    @Override
    public ExternalIdentity authenticate(Map<String, Object> credentials) {
        String idToken = Credentials.requiredRaw(credentials, "idToken", "identityToken", "token");
        Jwt jwt = googleJwtTokenService.decodeIdentityToken(idToken);

        String providerUserId = jwt.getSubject();
        if (providerUserId == null || providerUserId.isBlank()) {
            throw new InvalidCredentialsException("Google identity token missing subject.");
        }

        if (!googleJwtTokenService.isEmailVerified(jwt)) {
            throw new InvalidCredentialsException("Google account email is not verified.");
        }

        String email = normalize(jwt.getClaimAsString("email"));
        return new ExternalIdentity(AuthProviderType.GOOGLE, providerUserId, email);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase();
    }
}
