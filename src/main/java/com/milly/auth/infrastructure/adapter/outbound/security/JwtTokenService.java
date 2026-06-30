package com.milly.auth.infrastructure.adapter.outbound.security;

import com.milly.auth.application.model.AuthUser;
import com.milly.auth.domain.valueobject.RoleName;
import com.milly.auth.infrastructure.config.AuthProperties;
import com.milly.common.exception.InvalidCredentialsException;
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
public class JwtTokenService {

    public static final String ROLES_CLAIM = "roles";
    public static final String TOKEN_TYPE_CLAIM = "type";
    public static final String REFRESH_TOKEN_TYPE = "refresh";

    private final SecretKey signingKey;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public JwtTokenService(AuthProperties authProperties) {
        this.signingKey = Keys.hmacShaKeyFor(authProperties.jwt().secret().getBytes(StandardCharsets.UTF_8));
        this.accessTtl = Duration.ofSeconds(authProperties.jwt().accessTtlSeconds());
        this.refreshTtl = Duration.ofSeconds(authProperties.jwt().refreshTtlSeconds());
    }

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

    public String issueRefreshToken(AuthUser user) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(user.id().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(refreshTtl)))
                .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
                .signWith(signingKey)
                .compact();
    }

    public Claims parseAccessToken(String token) {
        Claims claims = parseClaims(token);
        if (REFRESH_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
            throw new InvalidCredentialsException("JWT token is expired or invalid.");
        }
        return claims;
    }

    public Claims parseRefreshToken(String token) {
        Claims claims = parseClaims(token);
        String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
        if (!REFRESH_TOKEN_TYPE.equals(tokenType)) {
            throw new InvalidCredentialsException("Invalid refresh token.");
        }
        return claims;
    }

    public boolean isValidToken(String token) {
        try {
            parseAccessToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    public UUID extractUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
