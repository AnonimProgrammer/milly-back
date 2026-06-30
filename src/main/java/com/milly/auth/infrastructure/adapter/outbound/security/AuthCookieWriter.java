package com.milly.auth.infrastructure.adapter.outbound.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;

public final class AuthCookieWriter {

    public static final String ACCESS_TOKEN_COOKIE = "access-token";
    public static final String REFRESH_TOKEN_COOKIE = "refresh-token";

    private AuthCookieWriter() {
    }

    public static void writeAuthCookies(
            HttpServletResponse response,
            String accessToken,
            String refreshToken,
            boolean secure) {
        response.addHeader("Set-Cookie", buildCookie(ACCESS_TOKEN_COOKIE, accessToken, secure).toString());
        response.addHeader("Set-Cookie", buildCookie(REFRESH_TOKEN_COOKIE, refreshToken, secure).toString());
    }

    private static ResponseCookie buildCookie(String name, String value, boolean secure) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .build();
    }
}
