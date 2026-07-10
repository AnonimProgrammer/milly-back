package com.milly.venue.infrastructure.adapter.inbound.http;

import com.milly.auth.application.polluter.AuthSession;
import com.milly.auth.application.polluter.AuthSessionPolluter;
import com.milly.config.domain.AbstractITest;
import com.milly.config.infrastructure.adapter.RestTestClientAuth;
import com.milly.venue.application.polluter.ManagedVenue;
import com.milly.venue.application.polluter.VenuePolluter;
import com.milly.venue.domain.valueobject.MemberStatus;
import com.milly.venue.domain.valueobject.VenueRole;
import com.milly.venue.infrastructure.adapter.inbound.http.dto.VenueMemberListApiResponse;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueMembershipJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

class VenueMemberRestIntegrationTest extends AbstractITest {

    @Autowired
    private RestTestClient restClient;

    @Autowired
    private VenuePolluter venuePolluter;

    @Autowired
    private AuthSessionPolluter authSessionPolluter;

    @Autowired
    private VenueMembershipJpaRepository venueMembershipRepository;

    @Test
    void managerListsVenueMembers() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        AuthSession waiter = venuePolluter.addMember(venue.venueId(), VenueRole.WAITER);
        RestTestClient managerClient = RestTestClientAuth.withSession(restClient, venue.manager());

        // Act
        VenueMemberListApiResponse response = managerClient.get()
                .uri("/api/v1/venues/{venueId}/members", venue.venueId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(VenueMemberListApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getMessage()).isEqualTo("Venue members retrieved successfully.");
        assertThat(response.getData().data()).hasSize(2);
        assertThat(response.getData().data())
                .anySatisfy(member -> {
                    assertThat(member.role()).isEqualTo(VenueRole.MANAGER);
                    assertThat(member.status()).isEqualTo(MemberStatus.ACTIVE);
                    assertThat(member.email()).isEqualTo(venue.manager().email());
                })
                .anySatisfy(member -> {
                    assertThat(member.role()).isEqualTo(VenueRole.WAITER);
                    assertThat(member.status()).isEqualTo(MemberStatus.ACTIVE);
                    assertThat(member.email()).isEqualTo(waiter.email());
                });
        assertThat(response.getData().pagination().limit()).isEqualTo(20);
        assertThat(response.getData().pagination().hasNext()).isFalse();
    }

    @Test
    void managerListsVenueMembersWithPagination() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        venuePolluter.addMember(venue.venueId(), VenueRole.WAITER);
        venuePolluter.addMember(venue.venueId(), VenueRole.WAITER);
        RestTestClient managerClient = RestTestClientAuth.withSession(restClient, venue.manager());

        // Act
        VenueMemberListApiResponse response = managerClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/venues/{venueId}/members")
                        .queryParam("limit", 1)
                        .build(venue.venueId()))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(VenueMemberListApiResponse.class)
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
    void waiterCannotListVenueMembers() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        AuthSession waiter = venuePolluter.addMember(venue.venueId(), VenueRole.WAITER);
        RestTestClient waiterClient = RestTestClientAuth.withSession(restClient, waiter);

        // Act & Assert
        waiterClient.get()
                .uri("/api/v1/venues/{venueId}/members", venue.venueId())
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody()
                .jsonPath("$.status").isEqualTo(403)
                .jsonPath("$.message").isEqualTo("Access denied.")
                .jsonPath("$.errorCode").isEqualTo("FORBIDDEN");
    }

    @Test
    void nonMemberCannotListVenueMembers() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        AuthSession nonMember = authSessionPolluter.registerPasswordUser();
        RestTestClient nonMemberClient = RestTestClientAuth.withSession(restClient, nonMember);

        // Act & Assert
        nonMemberClient.get()
                .uri("/api/v1/venues/{venueId}/members", venue.venueId())
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody()
                .jsonPath("$.status").isEqualTo(403)
                .jsonPath("$.errorCode").isEqualTo("FORBIDDEN");

        assertThat(venueMembershipRepository.findByUserIdAndVenueId(nonMember.userId(), venue.venueId()))
                .isEmpty();
    }

    @Test
    void listMembersReturnsBadRequestForInvalidCursor() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        RestTestClient managerClient = RestTestClientAuth.withSession(restClient, venue.manager());

        // Act & Assert
        managerClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/venues/{venueId}/members")
                        .queryParam("cursor", "not-a-page")
                        .build(venue.venueId()))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("BAD_REQUEST");
    }

    @Test
    void unauthenticatedListMembersReturnsUnauthorized() {
        // Act & Assert
        restClient.get()
                .uri("/api/v1/venues/{venueId}/members", java.util.UUID.randomUUID())
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.errorCode").isEqualTo("UNAUTHORIZED");
    }
}
