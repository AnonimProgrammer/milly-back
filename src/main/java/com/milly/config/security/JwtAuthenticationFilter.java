package com.milly.config.security;

import com.milly.auth.infrastructure.adapter.outbound.security.AuthCookieWriter;
import com.milly.auth.infrastructure.adapter.outbound.security.JwtTokenService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_ERROR_MESSAGE_ATTRIBUTE = "authErrorMessage";
    private static final String MISSING_AUTH_DETAILS_MESSAGE = "No authentication details were provided.";
    private static final String INVALID_OR_EXPIRED_TOKEN_MESSAGE = "JWT token is expired or invalid.";

    private final JwtTokenService jwtTokenService;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Optional<String> accessToken = extractAccessToken(request);
        if (accessToken.isEmpty()) {
            request.setAttribute(AUTH_ERROR_MESSAGE_ATTRIBUTE, MISSING_AUTH_DETAILS_MESSAGE);
            filterChain.doFilter(request, response);
            return;
        }

        String token = accessToken.get();
        if (!jwtTokenService.isValidToken(token)) {
            SecurityContextHolder.clearContext();
            request.setAttribute(AUTH_ERROR_MESSAGE_ATTRIBUTE, INVALID_OR_EXPIRED_TOKEN_MESSAGE);
            filterChain.doFilter(request, response);
            return;
        }

        Claims claims = jwtTokenService.parseAccessToken(token);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                jwtTokenService.extractUserId(claims), null, extractAuthorities(claims));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private Collection<SimpleGrantedAuthority> extractAuthorities(Claims claims) {
        Object value = claims.get(JwtTokenService.ROLES_CLAIM);
        if (!(value instanceof List<?> roles)) {
            return List.of();
        }
        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }

    private Optional<String> extractAccessToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie cookie : cookies) {
            if (AuthCookieWriter.ACCESS_TOKEN_COOKIE.equals(cookie.getName())
                    && cookie.getValue() != null
                    && !cookie.getValue().isBlank()) {
                return Optional.of(cookie.getValue());
            }
        }
        return Optional.empty();
    }
}
