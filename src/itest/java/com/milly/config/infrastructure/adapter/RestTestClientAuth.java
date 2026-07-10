package com.milly.config.infrastructure.adapter;

import com.milly.auth.application.polluter.AuthSession;
import com.milly.auth.infrastructure.adapter.outbound.security.AuthCookieWriter;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.List;

public final class RestTestClientAuth {

    public record AuthCookies(String accessToken, String refreshToken) {
    }

    private RestTestClientAuth() {
    }

    public static RestTestClient withSession(RestTestClient restClient, AuthSession session) {
        return restClient.mutate()
                .defaultCookie(AuthCookieWriter.ACCESS_TOKEN_COOKIE, session.accessToken())
                .build();
    }

    public static RestTestClient withAuthCookies(RestTestClient restClient, AuthCookies cookies) {
        return restClient.mutate()
                .defaultCookie(AuthCookieWriter.ACCESS_TOKEN_COOKIE, cookies.accessToken())
                .defaultCookie(AuthCookieWriter.REFRESH_TOKEN_COOKIE, cookies.refreshToken())
                .build();
    }

    public static AuthCookies parseAuthCookies(HttpHeaders headers) {
        List<String> setCookieHeaders = headers.getOrEmpty(HttpHeaders.SET_COOKIE);
        return new AuthCookies(
                extractCookieValue(setCookieHeaders, AuthCookieWriter.ACCESS_TOKEN_COOKIE),
                extractCookieValue(setCookieHeaders, AuthCookieWriter.REFRESH_TOKEN_COOKIE));
    }

    private static String extractCookieValue(List<String> setCookieHeaders, String cookieName) {
        String prefix = cookieName + "=";
        return setCookieHeaders.stream()
                .filter(header -> header.startsWith(prefix))
                .map(header -> {
                    int end = header.indexOf(';');
                    return end == -1 ? header.substring(prefix.length()) : header.substring(prefix.length(), end);
                })
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing cookie: " + cookieName));
    }
}
