package com.milly.auth.infrastructure.adapter.inbound.http;

import com.milly.auth.domain.valueobject.AuthProviderType;
import com.milly.auth.infrastructure.adapter.inbound.http.dto.ContinueAuthApiResponse;
import com.milly.config.domain.AbstractITest;
import com.milly.config.infrastructure.adapter.RestTestClientAuth;
import com.milly.config.infrastructure.adapter.RestTestClientAuth.AuthCookies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.Map;
import java.util.UUID;

class AuthRestIntegrationTest extends AbstractITest {

    private static final String DEFAULT_PASSWORD = "password123";

    @Autowired
    private RestTestClient restClient;

    private AuthCookies continuePasswordUser(String email, String password) {
        EntityExchangeResult<ContinueAuthApiResponse> result = restClient.post()
                .uri("/api/v1/auth/continue")
                .contentType(MediaType.APPLICATION_JSON)
                .body(passwordContinueBody(email, password))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ContinueAuthApiResponse.class)
                .returnResult();

        return RestTestClientAuth.parseAuthCookies(result.getResponseHeaders());
    }

    private static Map<String, Object> passwordContinueBody(String email, String password) {
        return Map.of(
                "provider", AuthProviderType.PASSWORD.name(),
                "credentials", Map.of(
                        "email", email,
                        "password", password),
                "profile", Map.of(
                        "firstName", "Test",
                        "lastName", "User",
                        "email", email));
    }

    private static String uniqueEmail() {
        return "itest-" + UUID.randomUUID() + "@example.com";
    }
}
