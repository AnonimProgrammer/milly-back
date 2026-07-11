package com.milly.auth.infrastructure.adapter.outbound.auth;

import com.milly.auth.infrastructure.config.AuthProperties;
import com.milly.common.application.exception.InvalidCredentialsException;
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
import org.springframework.stereotype.Service;

import java.net.URI;
import java.text.ParseException;

@Service
public class AppleJwtTokenService {

    private static final URI APPLE_JWKS_URI = URI.create("https://appleid.apple.com/auth/keys");
    private static final String APPLE_ISSUER = "https://appleid.apple.com";

    private final AuthProperties authProperties;
    private volatile ConfigurableJWTProcessor<SecurityContext> jwtProcessor;

    public AppleJwtTokenService(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    public JWTClaimsSet decodeIdentityToken(String idToken) {
        String clientId = authProperties.apple().clientId();
        if (clientId == null || clientId.isBlank()) {
            throw new UnsupportedOperationException("Apple authentication is not configured.");
        }

        return verifyToken(idToken, clientId);
    }

    private JWTClaimsSet verifyToken(String idToken, String clientId) {
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
}
