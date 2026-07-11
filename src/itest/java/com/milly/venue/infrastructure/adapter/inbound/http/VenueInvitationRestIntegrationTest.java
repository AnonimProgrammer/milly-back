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

    @Test
    void authenticatedUserRedeemsInvitationAndBecomesMember() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        RestTestClient managerClient = RestTestClientAuth.withSession(restClient, venue.manager());
        CreateVenueInvitationApiResponse invitation = managerClient.post()
                .uri("/api/v1/venues/{venueId}/invitations", venue.venueId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("role", "WAITER"))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(CreateVenueInvitationApiResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(invitation).isNotNull();

        AuthSession invitee = authSessionPolluter.registerPasswordUser();
        RestTestClient inviteeClient = RestTestClientAuth.withSession(restClient, invitee);

        // Act
        VenueMembershipApiResponse response = inviteeClient.post()
                .uri("/api/v1/invitations/redeem")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("token", invitation.getData().token()))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(VenueMembershipApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getMessage()).isEqualTo("Invitation redeemed successfully.");
        assertThat(response.getData().venueId()).isEqualTo(venue.venueId());
        assertThat(response.getData().venueName()).isEqualTo("Integration Test Venue");
        assertThat(response.getData().role()).isEqualTo(VenueRole.WAITER);
        assertThat(venueMembershipRepository.findByUserIdAndVenueId(invitee.userId(), venue.venueId()))
                .hasValueSatisfying(membership -> assertThat(membership.getRole()).isEqualTo(VenueRole.WAITER));
    }

    @Test
    void unauthenticatedRedeemReturnsUnauthorized() {
        // Act
        ErrorApiResponse response = restClient.post()
                .uri("/api/v1/invitations/redeem")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("token", UUID.randomUUID()))
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
    void redeemReturnsNotFoundForInvalidToken() {
        // Arrange
        AuthSession user = authSessionPolluter.registerPasswordUser();
        RestTestClient userClient = RestTestClientAuth.withSession(restClient, user);

        // Act
        ErrorApiResponse response = userClient.post()
                .uri("/api/v1/invitations/redeem")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("token", UUID.randomUUID()))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ErrorApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getMessage()).isEqualTo("Invitation is invalid or has expired.");
        assertThat(response.getErrorCode()).isEqualTo("NOT_FOUND");
    }

    @Test
    void redeemReturnsConflictWhenUserAlreadyMember() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        RestTestClient managerClient = RestTestClientAuth.withSession(restClient, venue.manager());
        CreateVenueInvitationApiResponse invitation = managerClient.post()
                .uri("/api/v1/venues/{venueId}/invitations", venue.venueId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("role", "WAITER"))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(CreateVenueInvitationApiResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(invitation).isNotNull();

        // Act
        ErrorApiResponse response = RestTestClientAuth.withSession(restClient, venue.manager()).post()
                .uri("/api/v1/invitations/redeem")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("token", invitation.getData().token()))
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectBody(ErrorApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(409);
        assertThat(response.getMessage()).isEqualTo("You are already a member of this venue.");
        assertThat(response.getErrorCode()).isEqualTo("CONFLICT");
    }

    @Test
    void redeemReturnsNotFoundWhenTokenAlreadyClaimed() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        RestTestClient managerClient = RestTestClientAuth.withSession(restClient, venue.manager());
        CreateVenueInvitationApiResponse invitation = managerClient.post()
                .uri("/api/v1/venues/{venueId}/invitations", venue.venueId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("role", "WAITER"))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(CreateVenueInvitationApiResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(invitation).isNotNull();

        AuthSession firstInvitee = authSessionPolluter.registerPasswordUser();
        RestTestClientAuth.withSession(restClient, firstInvitee).post()
                .uri("/api/v1/invitations/redeem")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("token", invitation.getData().token()))
                .exchange()
                .expectStatus()
                .isOk();

        AuthSession secondInvitee = authSessionPolluter.registerPasswordUser();
        RestTestClient secondInviteeClient = RestTestClientAuth.withSession(restClient, secondInvitee);

        // Act
        ErrorApiResponse response = secondInviteeClient.post()
                .uri("/api/v1/invitations/redeem")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("token", invitation.getData().token()))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ErrorApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getMessage()).isEqualTo("Invitation is invalid or has expired.");
        assertThat(response.getErrorCode()).isEqualTo("NOT_FOUND");
    }

    @Test
    void retryingCreateInvitationWithSameIdempotencyKeyReplaysResponse() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        RestTestClient managerClient = RestTestClientAuth.withSession(restClient, venue.manager());
        String idempotencyKey = UUID.randomUUID().toString();

        // Act
        CreateVenueInvitationApiResponse first = managerClient.post()
                .uri("/api/v1/venues/{venueId}/invitations", venue.venueId())
                .contentType(MediaType.APPLICATION_JSON)
                .header(IdempotencyAspect.IDEMPOTENCY_KEY_HEADER, idempotencyKey)
                .body(Map.of("role", "WAITER"))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(CreateVenueInvitationApiResponse.class)
                .returnResult()
                .getResponseBody();

        CreateVenueInvitationApiResponse second = managerClient.post()
                .uri("/api/v1/venues/{venueId}/invitations", venue.venueId())
                .contentType(MediaType.APPLICATION_JSON)
                .header(IdempotencyAspect.IDEMPOTENCY_KEY_HEADER, idempotencyKey)
                .body(Map.of("role", "WAITER"))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(CreateVenueInvitationApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(first).isNotNull();
        assertThat(second).isNotNull();
        assertThat(second.getData().token()).isEqualTo(first.getData().token());
        assertThat(second.getData().inviteUrl()).isEqualTo(first.getData().inviteUrl());
    }
}
