package com.milly.auth.infrastructure.adapter.inbound.http;

import com.milly.auth.application.exception.RefreshSessionFailedException;
import com.milly.auth.application.polluter.AuthSession;
import com.milly.auth.application.polluter.AuthSessionPolluter;
import com.milly.auth.domain.valueobject.AuthProviderType;
import com.milly.auth.infrastructure.adapter.outbound.auth.GoogleJwtTokenService;
import com.milly.auth.infrastructure.adapter.outbound.security.AuthCookieWriter;
import com.milly.auth.infrastructure.adapter.outbound.security.JwtTokenService;
import com.milly.auth.infrastructure.adapter.inbound.http.dto.ContinueAuthApiResponse;
import com.milly.auth.infrastructure.adapter.inbound.http.dto.CurrentUserApiResponse;
import com.milly.auth.infrastructure.adapter.inbound.http.dto.IssueWsTicketApiResponse;
import com.milly.config.domain.AbstractITest;
import com.milly.config.infrastructure.adapter.RestTestClientAuth;
import com.milly.config.infrastructure.adapter.RestTestClientAuth.AuthCookies;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthRestIntegrationTest extends AbstractITest {

    private static final String DEFAULT_PASSWORD = "password123";

    @Autowired
    private RestTestClient restClient;

    @Autowired
    private AuthSessionPolluter authSessionPolluter;

    @Autowired
    private GoogleJwtTokenService googleJwtTokenService;

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

    @Test
    void passwordContinueWithWrongPasswordReturnsUnauthorized() {
        // Arrange
        AuthSession existingUser = authSessionPolluter.registerPasswordUser();

        // Act & Assert
        restClient.post()
                .uri("/api/v1/auth/continue")
                .contentType(MediaType.APPLICATION_JSON)
                .body(passwordContinueBody(existingUser.email(), "wrong-password"))
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.errorCode").isEqualTo("UNAUTHORIZED")
                .jsonPath("$.message").isEqualTo("Invalid username or password.");
    }

    @Test
    void passwordContinueWithoutProfileForNewUserReturnsUnauthorized() {
        // Arrange
        String email = uniqueEmail();

        // Act & Assert
        restClient.post()
                .uri("/api/v1/auth/continue")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "provider", AuthProviderType.PASSWORD.name(),
                        "credentials", Map.of(
                                "email", email,
                                "password", DEFAULT_PASSWORD)))
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.errorCode").isEqualTo("UNAUTHORIZED")
                .jsonPath("$.message").isEqualTo("Profile data is required for first-time sign-in.");
    }

    @Test
    void continueReturnsBadRequestWhenProviderMissing() {
        // Act & Assert
        restClient.post()
                .uri("/api/v1/auth/continue")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "credentials", Map.of(
                                "email", uniqueEmail(),
                                "password", DEFAULT_PASSWORD),
                        "profile", Map.of(
                                "firstName", "Test",
                                "lastName", "User",
                                "email", uniqueEmail())))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("BAD_REQUEST")
                .jsonPath("$.message").isEqualTo("Provider is required.");
    }

    @Test
    void googleContinueRegistersNewUser() {
        // Arrange
        String email = uniqueEmail();
        String googleSubject = "google-" + UUID.randomUUID();
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(googleSubject);
        when(jwt.getClaimAsString("email")).thenReturn(email);
        when(googleJwtTokenService.decodeIdentityToken("google-id-token")).thenReturn(jwt);
        when(googleJwtTokenService.isEmailVerified(jwt)).thenReturn(true);

        // Act
        ContinueAuthApiResponse response = restClient.post()
                .uri("/api/v1/auth/continue")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "provider", AuthProviderType.GOOGLE.name(),
                        "credentials", Map.of("idToken", "google-id-token"),
                        "profile", Map.of(
                                "firstName", "Google",
                                "lastName", "User",
                                "email", email)))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ContinueAuthApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getData().newUser()).isTrue();
    }

    @Test
    void authenticatedGetMeReturnsCurrentUser() {
        // Arrange
        AuthSession user = authSessionPolluter.registerPasswordUser();
        RestTestClient userClient = RestTestClientAuth.withSession(restClient, user);

        // Act
        CurrentUserApiResponse response = userClient.get()
                .uri("/api/v1/auth/me")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(CurrentUserApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getMessage()).isEqualTo("Current user retrieved successfully.");
        assertThat(response.getData().id()).isEqualTo(user.userId());
        assertThat(response.getData().email()).isEqualTo(user.email());
        assertThat(response.getData().firstName()).isEqualTo("Test");
        assertThat(response.getData().lastName()).isEqualTo("User");
        assertThat(response.getData().roles()).containsExactly("USER");
    }

    @Test
    void unauthenticatedGetMeReturnsUnauthorized() {
        // Act & Assert
        restClient.get()
                .uri("/api/v1/auth/me")
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.errorCode").isEqualTo("UNAUTHORIZED")
                .jsonPath("$.message").isEqualTo("No authentication details were provided.");
    }

    @Test
    void invalidSessionGetMeReturnsUnauthorized() {
        // Arrange
        RestTestClient invalidClient = restClient.mutate()
                .defaultCookie(AuthCookieWriter.ACCESS_TOKEN_COOKIE, "not-a-valid-token")
                .build();

        // Act & Assert
        invalidClient.get()
                .uri("/api/v1/auth/me")
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.errorCode").isEqualTo("UNAUTHORIZED")
                .jsonPath("$.message").isEqualTo(JwtTokenService.INVALID_TOKEN_MESSAGE);
    }

    @Test
    void refreshSessionRotatesTokens() {
        // Arrange
        AuthCookies initialCookies = continuePasswordUser(uniqueEmail(), DEFAULT_PASSWORD);

        // Act
        EntityExchangeResult<Void> refreshResult = RestTestClientAuth.withAuthCookies(restClient, initialCookies)
                .post()
                .uri("/api/v1/auth/refresh")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(Void.class)
                .returnResult();

        AuthCookies rotatedCookies = RestTestClientAuth.parseAuthCookies(refreshResult.getResponseHeaders());

        // Assert
        assertThat(rotatedCookies.refreshToken()).isNotEqualTo(initialCookies.refreshToken());

        RestTestClientAuth.withAuthCookies(restClient, rotatedCookies)
                .get()
                .uri("/api/v1/auth/me")
                .exchange()
                .expectStatus()
                .isOk();

        RestTestClientAuth.withAuthCookies(restClient, initialCookies)
                .post()
                .uri("/api/v1/auth/refresh")
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    @Test
    void replayingConsumedRefreshTokenReturnsUnauthorized() {
        // Arrange
        AuthCookies initialCookies = continuePasswordUser(uniqueEmail(), DEFAULT_PASSWORD);
        RestTestClientAuth.withAuthCookies(restClient, initialCookies)
                .post()
                .uri("/api/v1/auth/refresh")
                .exchange()
                .expectStatus()
                .isOk();

        // Act & Assert
        RestTestClientAuth.withAuthCookies(restClient, initialCookies)
                .post()
                .uri("/api/v1/auth/refresh")
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.errorCode").isEqualTo("UNAUTHORIZED")
                .jsonPath("$.message").isEqualTo(RefreshSessionFailedException.MESSAGE);
    }

    @Test
    void refreshWithoutCookieReturnsUnauthorized() {
        // Act & Assert
        restClient.post()
                .uri("/api/v1/auth/refresh")
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.errorCode").isEqualTo("UNAUTHORIZED")
                .jsonPath("$.message").isEqualTo(RefreshSessionFailedException.MESSAGE);
    }

    @Test
    void logoutRevokesRefreshSession() {
        // Arrange
        AuthCookies cookies = continuePasswordUser(uniqueEmail(), DEFAULT_PASSWORD);
        RestTestClient authenticatedClient = RestTestClientAuth.withAuthCookies(restClient, cookies);

        // Act
        authenticatedClient.post()
                .uri("/api/v1/auth/logout")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.message").isEqualTo("Logged out.");

        // Assert
        RestTestClientAuth.withAuthCookies(restClient, cookies)
                .post()
                .uri("/api/v1/auth/refresh")
                .exchange()
                .expectStatus()
                .isUnauthorized();

        RestTestClientAuth.withSession(restClient, new AuthSession(
                        UUID.randomUUID(),
                        "unused@example.com",
                        DEFAULT_PASSWORD,
                        cookies.accessToken()))
                .get()
                .uri("/api/v1/auth/me")
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    void logoutWithoutCookiesReturnsOk() {
        // Act & Assert
        restClient.post()
                .uri("/api/v1/auth/logout")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.message").isEqualTo("Logged out.");
    }

    @Test
    void authenticatedIssueWsTicketReturnsTicket() {
        // Arrange
        AuthSession user = authSessionPolluter.registerPasswordUser();
        RestTestClient userClient = RestTestClientAuth.withSession(restClient, user);

        // Act
        IssueWsTicketApiResponse response = userClient.post()
                .uri("/api/v1/ws-ticket")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(IssueWsTicketApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getMessage()).isEqualTo("WebSocket ticket issued.");
        assertThat(response.getData().ticketId()).isNotNull();
        assertThat(response.getData().expiresAt()).isNotNull();
    }

    @Test
    void unauthenticatedIssueWsTicketReturnsUnauthorized() {
        // Act & Assert
        restClient.post()
                .uri("/api/v1/ws-ticket")
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.errorCode").isEqualTo("UNAUTHORIZED");
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
