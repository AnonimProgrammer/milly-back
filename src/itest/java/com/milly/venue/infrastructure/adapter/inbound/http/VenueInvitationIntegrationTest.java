package com.milly.venue.infrastructure.adapter.inbound.http;

import com.milly.auth.application.polluter.AuthSession;
import com.milly.auth.application.polluter.AuthSessionPolluter;
import com.milly.config.domain.AbstractITest;
import com.milly.config.infrastructure.adapter.RestTestClientAuth;
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

class VenueInvitationIntegrationTest extends AbstractITest {

    @Autowired
    private RestTestClient restClient;

    @Autowired
    private AuthSessionPolluter authSessionPolluter;

    @Autowired
    private VenuePolluter venuePolluter;

    @Autowired
    private VenueMembershipJpaRepository venueMembershipRepository;

    @Test
    void managerCreatesInvitation() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        RestTestClient managerClient = RestTestClientAuth.withSession(restClient, venue.manager());

        // Act
        CreateVenueInvitationApiResponse response = managerClient.post()
                .uri("/api/v1/venues/{venueId}/invitations", venue.venueId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("role", VenueRole.WAITER.name()))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(CreateVenueInvitationApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getData().token()).isNotNull();
        assertThat(response.getData().inviteUrl())
                .contains("/join-venue/invite/" + response.getData().token());
        assertThat(response.getData().role()).isEqualTo(VenueRole.WAITER);
    }

    @Test
    void unauthenticatedCreateReturnsUnauthorized() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();

        // Act & Assert
        restClient.post()
                .uri("/api/v1/venues/{venueId}/invitations", venue.venueId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("role", VenueRole.WAITER.name()))
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    @Test
    void waiterCreateReturnsForbidden() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        AuthSession waiter = venuePolluter.addMember(venue.venueId(), VenueRole.WAITER);
        RestTestClient waiterClient = RestTestClientAuth.withSession(restClient, waiter);

        // Act & Assert
        waiterClient.post()
                .uri("/api/v1/venues/{venueId}/invitations", venue.venueId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("role", VenueRole.WAITER.name()))
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    void redeemCreatesMembership() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        AuthSession invitee = authSessionPolluter.registerPasswordUser();
        UUID token = createInvitation(venue);
        RestTestClient inviteeClient = RestTestClientAuth.withSession(restClient, invitee);

        // Act
        VenueMembershipApiResponse response = inviteeClient.post()
                .uri("/api/v1/invitations/redeem")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("token", token.toString()))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(VenueMembershipApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getData().venueId()).isEqualTo(venue.venueId());
        assertThat(response.getData().role()).isEqualTo(VenueRole.WAITER);
        assertThat(venueMembershipRepository.findByUserIdAndVenueId(invitee.userId(), venue.venueId()))
                .isPresent();
    }

    @Test
    void redeemReuseReturnsNotFound() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        AuthSession invitee = authSessionPolluter.registerPasswordUser();
        UUID token = createInvitation(venue);
        RestTestClient inviteeClient = RestTestClientAuth.withSession(restClient, invitee);
        redeemInvitation(inviteeClient, token);

        // Act & Assert
        inviteeClient.post()
                .uri("/api/v1/invitations/redeem")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("token", token.toString()))
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void redeemWhenAlreadyMemberReturnsConflict() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        UUID token = createInvitation(venue);
        RestTestClient managerClient = RestTestClientAuth.withSession(restClient, venue.manager());

        // Act & Assert
        managerClient.post()
                .uri("/api/v1/invitations/redeem")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("token", token.toString()))
                .exchange()
                .expectStatus()
                .isEqualTo(409);
    }

    @Test
    void redeemWithInvalidTokenReturnsNotFound() {
        // Arrange
        AuthSession invitee = authSessionPolluter.registerPasswordUser();
        RestTestClient inviteeClient = RestTestClientAuth.withSession(restClient, invitee);

        // Act & Assert
        inviteeClient.post()
                .uri("/api/v1/invitations/redeem")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("token", UUID.randomUUID().toString()))
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void unauthenticatedRedeemReturnsUnauthorized() {
        // Act & Assert
        restClient.post()
                .uri("/api/v1/invitations/redeem")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("token", UUID.randomUUID().toString()))
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    private UUID createInvitation(ManagedVenue venue) {
        RestTestClient managerClient = RestTestClientAuth.withSession(restClient, venue.manager());
        CreateVenueInvitationApiResponse response = managerClient.post()
                .uri("/api/v1/venues/{venueId}/invitations", venue.venueId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("role", VenueRole.WAITER.name()))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(CreateVenueInvitationApiResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(response).isNotNull();
        return response.getData().token();
    }

    private void redeemInvitation(RestTestClient client, UUID token) {
        client.post()
                .uri("/api/v1/invitations/redeem")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("token", token.toString()))
                .exchange()
                .expectStatus()
                .isOk();
    }
}
