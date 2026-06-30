package com.milly.auth.infrastructure.adapter.outbound.auth;

import com.milly.auth.domain.model.ExternalIdentity;
import com.milly.auth.application.port.outbound.AuthProvider;
import com.milly.auth.domain.valueobject.AuthProviderType;
import com.milly.auth.infrastructure.config.AuthProperties;
import com.milly.auth.domain.Credentials;
import com.milly.common.exception.InvalidCredentialsException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.text.ParseException;
import java.util.Map;

@Component
public class AppleAuthProvider implements AuthProvider {

    private static final URI APPLE_JWKS_URI = URI.create("https://appleid.apple.com/auth/keys");
    private static final String APPLE_ISSUER = "https://appleid.apple.com";

    private final String clientId;
    private volatile ConfigurableJWTProcessor<SecurityContext> jwtProcessor;

    public AppleAuthProvider(AuthProperties authProperties) {
        this.clientId = authProperties.apple().clientId();
    }

    @Override
    public AuthProviderType getType() {
        return AuthProviderType.APPLE;
    }

    @Override
    public ExternalIdentity authenticate(Map<String, Object> credentials) {
        if (clientId == null || clientId.isBlank()) {
            throw new UnsupportedOperationException("Apple authentication is not configured.");
        }

        String idToken = Credentials.requiredRaw(credentials, "idToken", "identityToken", "token");
        JWTClaimsSet claims = verifyToken(idToken);

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

    private JWTClaimsSet verifyToken(String idToken) {
        try {
            SignedJWT signedJwt = SignedJWT.parse(idToken);
            JWTClaimsSet claims = processor().process(signedJwt, null);

            if (!APPLE_ISSUER.equals(claims.getIssuer())) {
                throw new InvalidCredentialsException("Invalid Apple identity token issuer.");
            }

            if (claims.getAudience() == null || !claims.getAudience().contains(clientId)) {
                throw new InvalidCredentialsException("Invalid Apple identity token audience.");
            }

            return claims;
        } catch (ParseException | BadJOSEException | JOSEException exception) {
            throw new InvalidCredentialsException("Invalid Apple identity token.");
        }
    }

    private ConfigurableJWTProcessor<SecurityContext> processor() {
        if (jwtProcessor == null) {
            synchronized (this) {
                if (jwtProcessor == null) {
                    jwtProcessor = buildProcessor();
                }
            }
        }
        return jwtProcessor;
    }

    private ConfigurableJWTProcessor<SecurityContext> buildProcessor() {
        try {
            JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(
                    JWSAlgorithm.RS256,
                    JWKSourceBuilder.create(APPLE_JWKS_URI.toURL()).build());
            DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
            processor.setJWSKeySelector(keySelector);
            return processor;
        } catch (java.net.MalformedURLException exception) {
            throw new IllegalStateException("Invalid Apple JWKS URI.", exception);
        }
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase();
    }
}
