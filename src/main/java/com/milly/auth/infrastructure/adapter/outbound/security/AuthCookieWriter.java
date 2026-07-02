package com.milly.auth.infrastructure.adapter.outbound.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.web.util.WebUtils;

import java.time.Duration;
import java.util.Optional;

public final class AuthCookieWriter {

    public static final String ACCESS_TOKEN_COOKIE = "access-token";
    public static final String REFRESH_TOKEN_COOKIE = "refresh-token";

    private AuthCookieWriter() {}

    public static void writeAuthCookies(
            HttpServletResponse response,
            String accessToken,
            String refreshToken,
            boolean secure) {
        response.addHeader("Set-Cookie", buildCookie(ACCESS_TOKEN_COOKIE, accessToken, secure).toString());
        response.addHeader("Set-Cookie", buildCookie(REFRESH_TOKEN_COOKIE, refreshToken, secure).toString());
    }

    public static void clearAuthCookies(HttpServletResponse response, boolean secure) {
        response.addHeader("Set-Cookie", buildClearCookie(ACCESS_TOKEN_COOKIE, secure).toString());
        response.addHeader("Set-Cookie", buildClearCookie(REFRESH_TOKEN_COOKIE, secure).toString());
    }

    public static Optional<String> readCookie(HttpServletRequest request, String cookieName) {
        Cookie cookie = WebUtils.getCookie(request, cookieName);
        if (cookie == null || cookie.getValue() == null || cookie.getValue().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(cookie.getValue());
    }

    private static ResponseCookie buildCookie(String name, String value, boolean secure) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .build();
    }

    private static ResponseCookie buildClearCookie(String name, boolean secure) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }
}
