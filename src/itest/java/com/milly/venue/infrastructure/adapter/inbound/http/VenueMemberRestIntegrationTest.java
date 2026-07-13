package com.milly.venue.infrastructure.adapter.inbound.http;

import com.milly.auth.application.polluter.AuthSession;
import com.milly.auth.application.polluter.AuthSessionPolluter;
import com.milly.config.domain.AbstractITest;
import com.milly.config.infrastructure.adapter.RestTestClientAuth;
import com.milly.venue.application.polluter.ManagedVenue;
import com.milly.venue.application.polluter.VenuePolluter;
import com.milly.venue.domain.valueobject.MemberStatus;
import com.milly.venue.domain.valueobject.VenueRole;
import com.milly.venue.infrastructure.adapter.inbound.http.dto.VenueMemberApiResponse;
import com.milly.venue.infrastructure.adapter.inbound.http.dto.VenueMemberListApiResponse;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueMembershipJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.Map;
import java.util.UUID;

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
        AuthSession waiter = venuePolluter.addMember(venue.venueId(), VenueRole.EMPLOYEE);
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
                    assertThat(member.role()).isEqualTo(VenueRole.OWNER);
                    assertThat(member.status()).isEqualTo(MemberStatus.ACTIVE);
                    assertThat(member.email()).isEqualTo(venue.manager().email());
                })
                .anySatisfy(member -> {
                    assertThat(member.role()).isEqualTo(VenueRole.EMPLOYEE);
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
        venuePolluter.addMember(venue.venueId(), VenueRole.EMPLOYEE);
        venuePolluter.addMember(venue.venueId(), VenueRole.EMPLOYEE);
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
        AuthSession waiter = venuePolluter.addMember(venue.venueId(), VenueRole.EMPLOYEE);
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

    @Test
    void managerBlocksEmployee() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        AuthSession manager = venuePolluter.addMember(venue.venueId(), VenueRole.MANAGER);
        AuthSession employee = venuePolluter.addMember(venue.venueId(), VenueRole.EMPLOYEE);
        UUID employeeMembershipId = membershipId(venue.venueId(), employee.userId());
        RestTestClient managerClient = RestTestClientAuth.withSession(restClient, manager);

        // Act
        VenueMemberApiResponse response = managerClient.patch()
                .uri("/api/v1/venues/{venueId}/members/{memberId}", venue.venueId(), employeeMembershipId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("status", "inactive"))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(VenueMemberApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getData().status()).isEqualTo(MemberStatus.INACTIVE);
        assertThat(response.getData().role()).isEqualTo(VenueRole.EMPLOYEE);
        assertThat(venueMembershipRepository.findById(employeeMembershipId))
                .hasValueSatisfying(membership -> assertThat(membership.getStatus()).isEqualTo(MemberStatus.INACTIVE));
    }

    @Test
    void managerCannotBlockAnotherManager() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        AuthSession manager = venuePolluter.addMember(venue.venueId(), VenueRole.MANAGER);
        AuthSession anotherManager = venuePolluter.addMember(venue.venueId(), VenueRole.MANAGER);
        UUID targetMembershipId = membershipId(venue.venueId(), anotherManager.userId());
        RestTestClient managerClient = RestTestClientAuth.withSession(restClient, manager);

        // Act & Assert
        managerClient.patch()
                .uri("/api/v1/venues/{venueId}/members/{memberId}", venue.venueId(), targetMembershipId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("status", "inactive"))
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody()
                .jsonPath("$.status").isEqualTo(403)
                .jsonPath("$.errorCode").isEqualTo("FORBIDDEN");
    }

    @Test
    void ownerDemotesManagerToEmployee() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        AuthSession manager = venuePolluter.addMember(venue.venueId(), VenueRole.MANAGER);
        UUID managerMembershipId = membershipId(venue.venueId(), manager.userId());
        RestTestClient ownerClient = RestTestClientAuth.withSession(restClient, venue.manager());

        // Act
        VenueMemberApiResponse response = ownerClient.patch()
                .uri("/api/v1/venues/{venueId}/members/{memberId}", venue.venueId(), managerMembershipId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("role", "EMPLOYEE"))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(VenueMemberApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getData().role()).isEqualTo(VenueRole.EMPLOYEE);
    }

    @Test
    void defaultListExcludesInactiveMembers() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        AuthSession employee = venuePolluter.addMember(venue.venueId(), VenueRole.EMPLOYEE);
        UUID employeeMembershipId = membershipId(venue.venueId(), employee.userId());
        RestTestClient ownerClient = RestTestClientAuth.withSession(restClient, venue.manager());

        ownerClient.patch()
                .uri("/api/v1/venues/{venueId}/members/{memberId}", venue.venueId(), employeeMembershipId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("status", "inactive"))
                .exchange()
                .expectStatus()
                .isOk();

        // Act
        VenueMemberListApiResponse response = ownerClient.get()
                .uri("/api/v1/venues/{venueId}/members", venue.venueId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(VenueMemberListApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getData().data()).hasSize(1);
        assertThat(response.getData().data().getFirst().role()).isEqualTo(VenueRole.OWNER);
    }

    @Test
    void listAllMembersIncludesInactiveOnes() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        AuthSession employee = venuePolluter.addMember(venue.venueId(), VenueRole.EMPLOYEE);
        UUID employeeMembershipId = membershipId(venue.venueId(), employee.userId());
        RestTestClient ownerClient = RestTestClientAuth.withSession(restClient, venue.manager());

        ownerClient.patch()
                .uri("/api/v1/venues/{venueId}/members/{memberId}", venue.venueId(), employeeMembershipId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("status", "inactive"))
                .exchange()
                .expectStatus()
                .isOk();

        // Act
        VenueMemberListApiResponse response = ownerClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/venues/{venueId}/members")
                        .queryParam("status", "all")
                        .build(venue.venueId()))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(VenueMemberListApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getData().data()).hasSize(2);
        assertThat(response.getData().data())
                .anySatisfy(member -> assertThat(member.status()).isEqualTo(MemberStatus.INACTIVE));
    }

    private UUID membershipId(UUID venueId, UUID userId) {
        return venueMembershipRepository.findByUserIdAndVenueId(userId, venueId)
                .orElseThrow()
                .getId();
    }
}
