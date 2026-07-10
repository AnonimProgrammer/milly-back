package com.milly.auth.infrastructure.adapter.outbound.security;

import com.milly.auth.application.port.outbound.SessionTokenPort;
import com.milly.auth.domain.model.AuthUser;
import com.milly.auth.domain.model.IssuedRefreshToken;
import com.milly.auth.domain.model.ParsedAccessToken;
import com.milly.auth.domain.model.ParsedRefreshToken;
import com.milly.auth.domain.valueobject.RoleName;
import com.milly.auth.infrastructure.config.AuthProperties;
import com.milly.common.application.exception.InvalidCredentialsException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class JwtTokenService implements SessionTokenPort {

    private static final String ROLES_CLAIM = "roles";
    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final SecretKey signingKey;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public JwtTokenService(AuthProperties authProperties) {
        this.signingKey = Keys.hmacShaKeyFor(authProperties.jwt().secret().getBytes(StandardCharsets.UTF_8));
        this.accessTtl = Duration.ofSeconds(authProperties.jwt().accessTtlSeconds());
        this.refreshTtl = Duration.ofSeconds(authProperties.jwt().refreshTtlSeconds());
    }

    @Override
    public String issueAccessToken(AuthUser user) {
        Instant now = Instant.now();
        List<String> roles = user.roles().stream().map(RoleName::name).toList();

        return Jwts.builder()
                .subject(user.id().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtl)))
                .claim(ROLES_CLAIM, roles)
                .signWith(signingKey)
                .compact();
    }

    @Override
    public IssuedRefreshToken issueRefreshToken(AuthUser user) {
        Instant now = Instant.now();
        String jti = UUID.randomUUID().toString();

        String token = Jwts.builder()
                .subject(user.id().toString())
                .id(jti)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(refreshTtl)))
                .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
                .signWith(signingKey)
                .compact();

        return new IssuedRefreshToken(token, jti);
    }

    @Override
    public boolean isValidAccessToken(String token) {
        try {
            parseAccessToken(token);
            return true;
        } catch (InvalidCredentialsException exception) {
            return false;
        }
    }

    @Override
    public ParsedAccessToken parseAccessToken(String token) {
        Claims claims = parseToken(token, false);
        return new ParsedAccessToken(extractUserId(claims), extractRoles(claims));
    }

    @Override
    public ParsedRefreshToken parseRefreshToken(String token) {
        Claims claims = parseToken(token, true);
        return new ParsedRefreshToken(extractUserId(claims), extractJti(claims));
    }

    private Claims parseToken(String token, boolean expectRefresh) {
        try {
            Claims claims = parseClaims(token);
            boolean isRefresh = REFRESH_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_CLAIM, String.class));
            if (isRefresh == expectRefresh) {
                return claims;
            }
        } catch (JwtException | IllegalArgumentException ignored) {
        }
        throw new InvalidCredentialsException(INVALID_TOKEN_MESSAGE);
    }

    private UUID extractUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    private String extractJti(Claims claims) {
        String jti = claims.getId();
        if (jti == null || jti.isBlank()) {
            throw new InvalidCredentialsException(INVALID_TOKEN_MESSAGE);
        }
        return jti;
    }

    private List<String> extractRoles(Claims claims) {
        Object value = claims.get(ROLES_CLAIM);
        if (!(value instanceof List<?> roles)) {
            return List.of();
        }
        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
