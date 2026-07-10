package com.milly.config.infrastructure.adapter.inbound.http;

import com.milly.auth.application.port.outbound.SessionTokenPort;
import com.milly.auth.domain.model.ParsedAccessToken;
import com.milly.auth.infrastructure.adapter.outbound.security.AuthCookieWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
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
    private static final String INVALID_OR_EXPIRED_TOKEN_MESSAGE = SessionTokenPort.INVALID_TOKEN_MESSAGE;

    private final SessionTokenPort sessionTokenPort;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        Optional<String> accessToken = AuthCookieWriter.readCookie(request, AuthCookieWriter.ACCESS_TOKEN_COOKIE);
        if (accessToken.isEmpty()) {
            request.setAttribute(AUTH_ERROR_MESSAGE_ATTRIBUTE, MISSING_AUTH_DETAILS_MESSAGE);
            filterChain.doFilter(request, response);
            return;
        }

        String token = accessToken.get();
        if (!sessionTokenPort.isValidAccessToken(token)) {
            SecurityContextHolder.clearContext();
            request.setAttribute(AUTH_ERROR_MESSAGE_ATTRIBUTE, INVALID_OR_EXPIRED_TOKEN_MESSAGE);
            filterChain.doFilter(request, response);
            return;
        }

        ParsedAccessToken parsed = sessionTokenPort.parseAccessToken(token);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                parsed.userId(), null, extractAuthorities(parsed.roles()));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private Collection<SimpleGrantedAuthority> extractAuthorities(List<String> roles) {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }
}
