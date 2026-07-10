package com.milly.config.infrastructure.adapter;

import com.milly.auth.application.polluter.AuthSession;
import com.milly.auth.infrastructure.adapter.outbound.security.AuthCookieWriter;
import org.springframework.test.web.servlet.client.RestTestClient;

public final class RestTestClientAuth {

    private RestTestClientAuth() {
    }

    public static RestTestClient withSession(RestTestClient restClient, AuthSession session) {
        return restClient.mutate()
                .defaultCookie(AuthCookieWriter.ACCESS_TOKEN_COOKIE, session.accessToken())
                .build();
    }
}