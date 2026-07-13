package com.milly.config.infrastructure.adapter.inbound.http;

import com.milly.auth.application.port.outbound.SessionTokenPort;
import com.milly.auth.application.usecase.LogoutUseCase;
import com.milly.auth.domain.entity.UserEntity;
import com.milly.auth.domain.model.ParsedAccessToken;
import com.milly.auth.domain.valueobject.UserStatus;
import com.milly.auth.infrastructure.adapter.outbound.persistence.UserJpaRepository;
import com.milly.auth.infrastructure.adapter.outbound.security.AuthCookieWriter;
import com.milly.common.application.exception.UserAccountInactiveException;
import com.milly.common.infrastructure.adapter.inbound.http.ErrorCode;
import com.milly.common.infrastructure.adapter.inbound.http.ServletErrorResponses;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

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
    private final UserJpaRepository userRepository;
    private final LogoutUseCase logoutUseCase;
    private final ObjectMapper objectMapper;

    @Value("${auth.cookies.secure:false}")
    private boolean secureCookies;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return "/api/v1/auth/refresh".equals(path) || "/api/v1/auth/logout".equals(path);
    }

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
        Optional<UserEntity> user = userRepository.findById(parsed.userId());
        if (user.isEmpty()) {
            SecurityContextHolder.clearContext();
            request.setAttribute(AUTH_ERROR_MESSAGE_ATTRIBUTE, INVALID_OR_EXPIRED_TOKEN_MESSAGE);
            filterChain.doFilter(request, response);
            return;
        }

        if (user.get().getStatus() != UserStatus.ACTIVE) {
            terminateSession(request, response);
            ServletErrorResponses.write(
                    response,
                    objectMapper,
                    HttpStatus.FORBIDDEN,
                    ErrorCode.ACCOUNT_INACTIVE,
                    UserAccountInactiveException.MESSAGE);
            return;
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                parsed.userId(), null, extractAuthorities(parsed.roles()));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private void terminateSession(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        AuthCookieWriter.readCookie(request, AuthCookieWriter.REFRESH_TOKEN_COOKIE)
                .ifPresent(logoutUseCase::execute);
        AuthCookieWriter.clearAuthCookies(response, secureCookies);
    }

    private Collection<SimpleGrantedAuthority> extractAuthorities(List<String> roles) {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }
}
