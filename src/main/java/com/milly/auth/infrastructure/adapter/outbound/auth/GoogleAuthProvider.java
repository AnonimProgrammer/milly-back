package com.milly.auth.infrastructure.adapter.outbound.auth;

import com.milly.auth.application.port.outbound.AuthProvider;
import com.milly.auth.domain.Credentials;
import com.milly.auth.domain.model.ExternalIdentity;
import com.milly.auth.domain.valueobject.AuthProviderType;
import com.milly.common.application.exception.InvalidCredentialsException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class GoogleAuthProvider implements AuthProvider {

    private final GoogleJwtTokenService googleJwtTokenService;

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
