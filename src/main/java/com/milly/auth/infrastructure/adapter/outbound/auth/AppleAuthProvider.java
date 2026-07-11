package com.milly.auth.infrastructure.adapter.outbound.auth;

import com.milly.auth.application.port.outbound.AuthProvider;
import com.milly.auth.domain.Credentials;
import com.milly.auth.domain.model.ExternalIdentity;
import com.milly.auth.domain.valueobject.AuthProviderType;
import com.milly.common.application.exception.InvalidCredentialsException;
import com.nimbusds.jwt.JWTClaimsSet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AppleAuthProvider implements AuthProvider {

    private final AppleJwtTokenService appleJwtTokenService;

    @Override
    public AuthProviderType getType() {
        return AuthProviderType.APPLE;
    }

    @Override
    public ExternalIdentity authenticate(Map<String, Object> credentials) {
        String idToken = Credentials.requiredRaw(credentials, "idToken", "identityToken", "token");
        JWTClaimsSet claims = appleJwtTokenService.decodeIdentityToken(idToken);

        String providerUserId = claims.getSubject();
        if (providerUserId == null || providerUserId.isBlank()) {
            throw new InvalidCredentialsException("Apple identity token missing subject.");
        }

        String email = normalizeEmailClaim(claims);
        return new ExternalIdentity(AuthProviderType.APPLE, providerUserId, email);
    }

    private static String normalizeEmailClaim(JWTClaimsSet claims) {
        try {
            return normalize(claims.getStringClaim("email"));
        } catch (ParseException exception) {
            return null;
        }
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase();
    }
}
