package com.milly.config.security;

import com.milly.auth.infrastructure.adapter.outbound.security.AuthCookieWriter;
import com.milly.auth.infrastructure.adapter.outbound.security.JwtTokenService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_ERROR_MESSAGE_ATTRIBUTE = "authErrorMessage";
    private static final String MISSING_AUTH_DETAILS_MESSAGE = "No authentication details were provided.";
    private static final String INVALID_OR_EXPIRED_TOKEN_MESSAGE = JwtTokenService.INVALID_TOKEN_MESSAGE;

    private final JwtTokenService jwtTokenService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Optional<String> accessToken = AuthCookieWriter.readCookie(request, AuthCookieWriter.ACCESS_TOKEN_COOKIE);
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

        Claims claims = jwtTokenService.parseToken(token, false);
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
}
