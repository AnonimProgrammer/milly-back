package com.milly.venue.infrastructure.adapter.inbound.http;

import com.milly.auth.application.polluter.AuthSession;
import com.milly.auth.application.polluter.AuthSessionPolluter;
import com.milly.common.infrastructure.adapter.inbound.idempotency.IdempotencyAspect;
import com.milly.config.domain.AbstractITest;
import com.milly.config.infrastructure.adapter.RestTestClientAuth;
import com.milly.config.infrastructure.adapter.dto.ErrorApiResponse;
import com.milly.venue.application.polluter.ManagedVenue;
import com.milly.venue.application.polluter.VenuePolluter;
import com.milly.venue.domain.valueobject.VenueRole;
import com.milly.venue.infrastructure.adapter.inbound.http.dto.CreateVenueInvitationApiResponse;
import com.milly.venue.infrastructure.adapter.inbound.http.dto.VenueMembershipApiResponse;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueMembershipJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VenueInvitationRestIntegrationTest extends AbstractITest {

    @Autowired
    private RestTestClient restClient;

    @Autowired
    private VenuePolluter venuePolluter;

    @Autowired
    private AuthSessionPolluter authSessionPolluter;

    @Autowired
    private VenueMembershipJpaRepository venueMembershipRepository;

    @Test
    void managerCreatesInvitationForWaiterRole() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        RestTestClient managerClient = RestTestClientAuth.withSession(restClient, venue.manager());

        // Act
        CreateVenueInvitationApiResponse response = managerClient.post()
                .uri("/api/v1/venues/{venueId}/invitations", venue.venueId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("role", "WAITER"))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(CreateVenueInvitationApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(201);
        assertThat(response.getMessage()).isEqualTo("Invitation created successfully.");
        assertThat(response.getData().token()).isNotNull();
        assertThat(response.getData().role()).isEqualTo(VenueRole.WAITER);
        assertThat(response.getData().inviteUrl())
                .isEqualTo("http://localhost:3000/join-venue/invite/" + response.getData().token());
    }

    @Test
    void unauthenticatedCreateInvitationReturnsUnauthorized() {
        // Act
        ErrorApiResponse response = restClient.post()
                .uri("/api/v1/venues/{venueId}/invitations", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("role", "WAITER"))
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody(ErrorApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getErrorCode()).isEqualTo("UNAUTHORIZED");
    }

    @Test
    void waiterCannotCreateInvitation() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        AuthSession waiter = venuePolluter.addMember(venue.venueId(), VenueRole.WAITER);
        RestTestClient waiterClient = RestTestClientAuth.withSession(restClient, waiter);

        // Act
        ErrorApiResponse response = waiterClient.post()
                .uri("/api/v1/venues/{venueId}/invitations", venue.venueId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("role", "WAITER"))
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody(ErrorApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getMessage()).isEqualTo("Access denied.");
        assertThat(response.getErrorCode()).isEqualTo("FORBIDDEN");
    }

    @Test
    void nonMemberCannotCreateInvitation() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        AuthSession outsider = authSessionPolluter.registerPasswordUser();
        RestTestClient outsiderClient = RestTestClientAuth.withSession(restClient, outsider);

        // Act
        ErrorApiResponse response = outsiderClient.post()
                .uri("/api/v1/venues/{venueId}/invitations", venue.venueId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("role", "WAITER"))
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody(ErrorApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getErrorCode()).isEqualTo("FORBIDDEN");
    }

    @Test
    void createInvitationReturnsForbiddenWhenUserHasNoMembershipForVenue() {
        // Arrange
        AuthSession manager = authSessionPolluter.registerPasswordUser();
        RestTestClient managerClient = RestTestClientAuth.withSession(restClient, manager);
        UUID missingVenueId = UUID.randomUUID();

        // Act
        ErrorApiResponse response = managerClient.post()
                .uri("/api/v1/venues/{venueId}/invitations", missingVenueId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("role", "WAITER"))
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody(ErrorApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getErrorCode()).isEqualTo("FORBIDDEN");
    }

    @Test
    void createInvitationReturnsBadRequestWhenRoleMissing() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        RestTestClient managerClient = RestTestClientAuth.withSession(restClient, venue.manager());

        // Act
        ErrorApiResponse response = managerClient.post()
                .uri("/api/v1/venues/{venueId}/invitations", venue.venueId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of())
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ErrorApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getMessage()).isEqualTo("Role is required.");
        assertThat(response.getErrorCode()).isEqualTo("BAD_REQUEST");
    }
}
