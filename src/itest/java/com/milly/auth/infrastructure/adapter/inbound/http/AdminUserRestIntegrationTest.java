package com.milly.auth.infrastructure.adapter.inbound.http;

import com.milly.auth.application.dto.AdminUserResponse;
import com.milly.auth.application.polluter.AuthSession;
import com.milly.auth.application.polluter.AuthSessionPolluter;
import com.milly.auth.domain.valueobject.UserStatus;
import com.milly.auth.infrastructure.adapter.inbound.http.dto.AdminUserApiResponse;
import com.milly.auth.infrastructure.adapter.inbound.http.dto.AdminUserListApiResponse;
import com.milly.config.domain.AbstractITest;
import com.milly.config.infrastructure.adapter.RestTestClientAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AdminUserRestIntegrationTest extends AbstractITest {

    @Autowired
    private RestTestClient restClient;

    @Autowired
    private AuthSessionPolluter authSessionPolluter;

    @Test
    void adminListsUsers() {
        // Arrange
        AuthSession admin = authSessionPolluter.registerAdminUser();
        AuthSession regularUser = authSessionPolluter.registerPasswordUser();
        RestTestClient adminClient = RestTestClientAuth.withSession(restClient, admin);

        // Act
        AdminUserListApiResponse response = adminClient.get()
                .uri("/api/v1/admin/users")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(AdminUserListApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getMessage()).isEqualTo("Users retrieved successfully.");
        assertThat(response.getData().data())
                .extracting(AdminUserResponse::email)
                .contains(admin.email(), regularUser.email());
        assertThat(response.getData().data())
                .filteredOn(user -> user.email().equals(admin.email()))
                .singleElement()
                .satisfies(user -> assertThat(user.roles()).contains("ADMIN", "USER"));
        assertThat(response.getData().pagination().limit()).isEqualTo(20);
        assertThat(response.getData().pagination().hasPrevious()).isFalse();
    }

    @Test
    void adminListsUsersWithPagination() {
        // Arrange
        AuthSession admin = authSessionPolluter.registerAdminUser();
        authSessionPolluter.registerPasswordUser();
        authSessionPolluter.registerPasswordUser();
        RestTestClient adminClient = RestTestClientAuth.withSession(restClient, admin);

        // Act
        AdminUserListApiResponse response = adminClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/admin/users")
                        .queryParam("limit", 1)
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(AdminUserListApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getData().data()).hasSize(1);
        assertThat(response.getData().pagination().limit()).isEqualTo(1);
        assertThat(response.getData().pagination().hasNext()).isTrue();
        assertThat(response.getData().pagination().nextCursor()).isEqualTo("1");
    }

    @Test
    void adminFiltersUsersByEmail() {
        // Arrange
        AuthSession admin = authSessionPolluter.registerAdminUser();
        AuthSession target = authSessionPolluter.registerPasswordUser();
        authSessionPolluter.registerPasswordUser();
        RestTestClient adminClient = RestTestClientAuth.withSession(restClient, admin);

        // Act
        AdminUserListApiResponse response = adminClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/admin/users")
                        .queryParam("email", target.email())
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(AdminUserListApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getData().data())
                .extracting(AdminUserResponse::email)
                .containsExactly(target.email());
    }

    @Test
    void adminFiltersUsersByStatusAndRole() {
        // Arrange
        AuthSession admin = authSessionPolluter.registerAdminUser();
        authSessionPolluter.registerPasswordUser();
        RestTestClient adminClient = RestTestClientAuth.withSession(restClient, admin);

        // Act
        AdminUserListApiResponse response = adminClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/admin/users")
                        .queryParam("status", "ACTIVE")
                        .queryParam("role", "ADMIN")
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(AdminUserListApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getData().data())
                .isNotEmpty()
                .allSatisfy(user -> {
                    assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
                    assertThat(user.roles()).contains("ADMIN");
                });
    }

    @Test
    void adminUpdatesUserStatusAndRoles() {
        // Arrange
        AuthSession admin = authSessionPolluter.registerAdminUser();
        AuthSession target = authSessionPolluter.registerPasswordUser();
        RestTestClient adminClient = RestTestClientAuth.withSession(restClient, admin);

        // Act
        AdminUserApiResponse response = adminClient.patch()
                .uri("/api/v1/admin/users/{userId}", target.userId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "status", "SUSPENDED",
                        "roles", List.of("ADMIN")))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(AdminUserApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getMessage()).isEqualTo("User updated successfully.");
        assertThat(response.getData().id()).isEqualTo(target.userId());
        assertThat(response.getData().status()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(response.getData().roles()).containsExactlyInAnyOrder("USER", "ADMIN");
    }

    @Test
    void adminCannotDeactivateSelf() {
        // Arrange
        AuthSession admin = authSessionPolluter.registerAdminUser();
        RestTestClient adminClient = RestTestClientAuth.withSession(restClient, admin);

        // Act & Assert
        adminClient.patch()
                .uri("/api/v1/admin/users/{userId}", admin.userId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("status", "INACTIVE"))
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody()
                .jsonPath("$.status").isEqualTo(403)
                .jsonPath("$.message").isEqualTo("Access denied.")
                .jsonPath("$.errorCode").isEqualTo("FORBIDDEN");
    }

    @Test
    void nonAdminCannotListUsers() {
        // Arrange
        AuthSession regularUser = authSessionPolluter.registerPasswordUser();
        RestTestClient userClient = RestTestClientAuth.withSession(restClient, regularUser);

        // Act & Assert
        userClient.get()
                .uri("/api/v1/admin/users")
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody()
                .jsonPath("$.status").isEqualTo(403)
                .jsonPath("$.message").isEqualTo("Access denied.")
                .jsonPath("$.errorCode").isEqualTo("FORBIDDEN");
    }

    @Test
    void unauthenticatedRequestCannotListUsers() {
        // Arrange
        // no session cookies

        // Act & Assert
        restClient.get()
                .uri("/api/v1/admin/users")
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }
}
