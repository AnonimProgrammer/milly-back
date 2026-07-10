package com.milly.auth.infrastructure.adapter.inbound.http;

import com.milly.auth.application.polluter.AuthSession;
import com.milly.auth.application.polluter.AuthSessionPolluter;
import com.milly.auth.domain.valueobject.AuthProviderType;
import com.milly.auth.infrastructure.adapter.inbound.http.dto.ContinueAuthApiResponse;
import com.milly.auth.infrastructure.adapter.inbound.http.dto.CurrentUserApiResponse;
import com.milly.config.domain.AbstractITest;
import com.milly.config.infrastructure.adapter.RestTestClientAuth;
import com.milly.config.infrastructure.adapter.RestTestClientAuth.AuthCookies;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuthRestIntegrationTest extends AbstractITest {

    private static final String DEFAULT_PASSWORD = "password123";

    @Autowired
    private RestTestClient restClient;

    @Autowired
    private AuthSessionPolluter authSessionPolluter;

    @Test
    void passwordContinueRegistersNewUserAndSetsAuthCookies() {
        // Arrange
        String email = uniqueEmail();

        // Act
        EntityExchangeResult<ContinueAuthApiResponse> result = restClient.post()
                .uri("/api/v1/auth/continue")
                .contentType(MediaType.APPLICATION_JSON)
                .body(passwordContinueBody(email, DEFAULT_PASSWORD))
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .exists(HttpHeaders.SET_COOKIE)
                .expectBody(ContinueAuthApiResponse.class)
                .returnResult();

        ContinueAuthApiResponse response = result.getResponseBody();
        AuthCookies cookies = RestTestClientAuth.parseAuthCookies(result.getResponseHeaders());

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getMessage()).isEqualTo("Authentication successful.");
        assertThat(response.getData().newUser()).isTrue();
        assertThat(cookies.accessToken()).isNotBlank();
        assertThat(cookies.refreshToken()).isNotBlank();

        CurrentUserApiResponse currentUser = RestTestClientAuth.withAuthCookies(restClient, cookies)
                .get()
                .uri("/api/v1/auth/me")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(CurrentUserApiResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(currentUser.getData().email()).isEqualTo(email);
        assertThat(currentUser.getData().firstName()).isEqualTo("Test");
        assertThat(currentUser.getData().lastName()).isEqualTo("User");
        assertThat(currentUser.getData().roles()).containsExactly("USER");
    }

    @Test
    void passwordContinueLogsInExistingUser() {
        // Arrange
        AuthSession existingUser = authSessionPolluter.registerPasswordUser();

        // Act
        ContinueAuthApiResponse response = restClient.post()
                .uri("/api/v1/auth/continue")
                .contentType(MediaType.APPLICATION_JSON)
                .body(passwordContinueBody(existingUser.email(), existingUser.password()))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ContinueAuthApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getData().newUser()).isFalse();
    }

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
