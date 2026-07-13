package com.milly.venue.infrastructure.adapter.inbound.http;

import com.milly.auth.application.polluter.AuthSession;
import com.milly.auth.application.polluter.AuthSessionPolluter;
import com.milly.auth.infrastructure.adapter.outbound.security.AuthCookieWriter;
import com.milly.config.domain.AbstractITest;
import com.milly.config.infrastructure.adapter.RestTestClientAuth;
import com.milly.venue.application.polluter.ManagedVenue;
import com.milly.venue.application.polluter.VenuePolluter;
import com.milly.venue.application.dto.VenueMembershipResponse;
import com.milly.venue.domain.valueobject.MemberStatus;
import com.milly.venue.domain.valueobject.VenueRole;
import com.milly.venue.domain.valueobject.VenueStatus;
import com.milly.venue.infrastructure.adapter.inbound.http.dto.CreateVenueApiResponse;
import com.milly.venue.infrastructure.adapter.inbound.http.dto.VenueMembershipApiResponse;
import com.milly.venue.infrastructure.adapter.inbound.http.dto.VenueMembershipListApiResponse;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueJpaRepository;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueMembershipJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VenueRestIntegrationTest extends AbstractITest {

    @Autowired
    private RestTestClient restClient;

    @Autowired
    private AuthSessionPolluter authSessionPolluter;

    @Autowired
    private VenuePolluter venuePolluter;

    @Autowired
    private VenueJpaRepository venueRepository;

    @Autowired
    private VenueMembershipJpaRepository venueMembershipRepository;

    @Test
    void authenticatedUserCreatesVenueAndOwnerMembership() {
        // Arrange
        AuthSession manager = authSessionPolluter.registerPasswordUser();
        RestTestClient managerClient = RestTestClientAuth.withSession(restClient, manager);

        // Act
        CreateVenueApiResponse response = managerClient.post()
                .uri("/api/v1/venues")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "name", "  Integration Venue  ",
                        "location", "  Test City  "))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(CreateVenueApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(201);
        assertThat(response.getMessage()).isEqualTo("Venue created successfully.");
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getData().id()).isNotNull();
        assertThat(response.getData().name()).isEqualTo("Integration Venue");
        assertThat(response.getData().location()).isEqualTo("Test City");
        assertThat(response.getData().role()).isEqualTo(VenueRole.OWNER);

        assertThat(venueRepository.findById(response.getData().id()))
                .hasValueSatisfying(venue -> {
                    assertThat(venue.getName()).isEqualTo("Integration Venue");
                    assertThat(venue.getLocation()).isEqualTo("Test City");
                    assertThat(venue.getStatus()).isEqualTo(VenueStatus.ACTIVE);
                });
        assertThat(venueMembershipRepository.findByUserIdAndVenueId(manager.userId(), response.getData().id()))
                .hasValueSatisfying(membership -> {
                    assertThat(membership.getUserId()).isEqualTo(manager.userId());
                    assertThat(membership.getVenueId()).isEqualTo(response.getData().id());
                    assertThat(membership.getRole()).isEqualTo(VenueRole.OWNER);
                });
    }

    @Test
    void unauthenticatedCreateVenueReturnsUnauthorized() {
        // Arrange
        long venueCount = venueRepository.count();

        // Act & Assert
        restClient.post()
                .uri("/api/v1/venues")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "name", "Integration Venue",
                        "location", "Test City"))
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.errorCode").isEqualTo("UNAUTHORIZED");

        assertThat(venueRepository.count()).isEqualTo(venueCount);
    }

    @Test
    void createVenueReturnsBadRequestWhenNameIsBlank() {
        // Arrange
        AuthSession manager = authSessionPolluter.registerPasswordUser();
        RestTestClient managerClient = RestTestClientAuth.withSession(restClient, manager);
        long venueCount = venueRepository.count();

        // Act & Assert
        managerClient.post()
                .uri("/api/v1/venues")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "name", " ",
                        "location", "Test City"))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("Name is required.")
                .jsonPath("$.errorCode").isEqualTo("BAD_REQUEST");

        assertThat(venueRepository.count()).isEqualTo(venueCount);
    }

    @Test
    void createVenueReturnsBadRequestWhenLocationIsBlank() {
        // Arrange
        AuthSession manager = authSessionPolluter.registerPasswordUser();
        RestTestClient managerClient = RestTestClientAuth.withSession(restClient, manager);
        long venueCount = venueRepository.count();

        // Act & Assert
        managerClient.post()
                .uri("/api/v1/venues")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "name", "Integration Venue",
                        "location", " "))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("Location is required.")
                .jsonPath("$.errorCode").isEqualTo("BAD_REQUEST");

        assertThat(venueRepository.count()).isEqualTo(venueCount);
    }

    @Test
    void createVenueReturnsBadRequestWhenRequiredFieldsAreMissing() {
        // Arrange
        AuthSession manager = authSessionPolluter.registerPasswordUser();
        RestTestClient managerClient = RestTestClientAuth.withSession(restClient, manager);
        long venueCount = venueRepository.count();

        // Act & Assert
        managerClient.post()
                .uri("/api/v1/venues")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of())
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("BAD_REQUEST");

        assertThat(venueRepository.count()).isEqualTo(venueCount);
    }

    @Test
    void managerGetsOwnVenueMembership() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        RestTestClient managerClient = RestTestClientAuth.withSession(restClient, venue.manager());

        // Act
        VenueMembershipApiResponse response = managerClient.get()
                .uri("/api/v1/venues/{venueId}/me", venue.venueId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(VenueMembershipApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getMessage()).isEqualTo("Venue membership retrieved successfully.");
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getData().venueId()).isEqualTo(venue.venueId());
        assertThat(response.getData().venueName()).isEqualTo("Integration Test Venue");
        assertThat(response.getData().location()).isEqualTo("Test City");
        assertThat(response.getData().role()).isEqualTo(VenueRole.OWNER);
        assertThat(response.getData().status()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(venueMembershipRepository.findByUserIdAndVenueId(venue.manager().userId(), venue.venueId()))
                .hasValueSatisfying(membership -> assertThat(membership.getRole()).isEqualTo(VenueRole.OWNER));
    }

    @Test
    void waiterGetsOwnVenueMembership() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        AuthSession waiter = venuePolluter.addMember(venue.venueId(), VenueRole.EMPLOYEE);
        RestTestClient waiterClient = RestTestClientAuth.withSession(restClient, waiter);

        // Act
        VenueMembershipApiResponse response = waiterClient.get()
                .uri("/api/v1/venues/{venueId}/me", venue.venueId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(VenueMembershipApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getData().venueId()).isEqualTo(venue.venueId());
        assertThat(response.getData().venueName()).isEqualTo("Integration Test Venue");
        assertThat(response.getData().location()).isEqualTo("Test City");
        assertThat(response.getData().role()).isEqualTo(VenueRole.EMPLOYEE);
        assertThat(venueMembershipRepository.findByUserIdAndVenueId(waiter.userId(), venue.venueId()))
                .hasValueSatisfying(membership -> assertThat(membership.getRole()).isEqualTo(VenueRole.EMPLOYEE));
    }

    @Test
    void authenticatedUserListsOwnVenues() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        RestTestClient managerClient = RestTestClientAuth.withSession(restClient, venue.manager());

        // Act
        VenueMembershipListApiResponse response = managerClient.get()
                .uri("/api/v1/venues")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(VenueMembershipListApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getMessage()).isEqualTo("Venues retrieved successfully.");
        assertThat(response.getTimestamp()).isNotNull();
        List<VenueMembershipResponse> memberships = response.getData();
        assertThat(memberships)
                .filteredOn(membership -> membership.venueId().equals(venue.venueId()))
                .singleElement()
                .satisfies(membership -> {
                    assertThat(membership.venueName()).isEqualTo("Integration Test Venue");
                    assertThat(membership.location()).isEqualTo("Test City");
                    assertThat(membership.role()).isEqualTo(VenueRole.OWNER);
                });
    }

    @Test
    void unauthenticatedListVenuesReturnsUnauthorized() {
        // Act & Assert
        restClient.get()
                .uri("/api/v1/venues")
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.errorCode").isEqualTo("UNAUTHORIZED");
    }

    @Test
    void unauthenticatedGetMembershipReturnsUnauthorized() {
        // Act & Assert
        restClient.get()
                .uri("/api/v1/venues/{venueId}/me", java.util.UUID.randomUUID())
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.errorCode").isEqualTo("UNAUTHORIZED");
    }

    @Test
    void invalidSessionGetMembershipReturnsUnauthorized() {
        // Arrange
        RestTestClient invalidClient = restClient.mutate()
                .defaultCookie(AuthCookieWriter.ACCESS_TOKEN_COOKIE, "not-a-valid-token")
                .build();

        // Act & Assert
        invalidClient.get()
                .uri("/api/v1/venues/{venueId}/me", java.util.UUID.randomUUID())
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.errorCode").isEqualTo("UNAUTHORIZED");
    }

    @Test
    void getMembershipReturnsForbiddenWhenUserIsNotVenueMember() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        AuthSession nonMember = authSessionPolluter.registerPasswordUser();
        RestTestClient nonMemberClient = RestTestClientAuth.withSession(restClient, nonMember);

        // Act & Assert
        nonMemberClient.get()
                .uri("/api/v1/venues/{venueId}/me", venue.venueId())
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody()
                .jsonPath("$.status").isEqualTo(403)
                .jsonPath("$.message").isEqualTo("Access denied.")
                .jsonPath("$.errorCode").isEqualTo("FORBIDDEN");

        assertThat(venueMembershipRepository.findByUserIdAndVenueId(nonMember.userId(), venue.venueId()))
                .isEmpty();
    }

    @Test
    void getMembershipReturnsForbiddenWhenUserIsBlocked() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        AuthSession employee = venuePolluter.addMember(venue.venueId(), VenueRole.EMPLOYEE);
        UUID employeeMembershipId = venueMembershipRepository.findByUserIdAndVenueId(employee.userId(), venue.venueId())
                .orElseThrow()
                .getId();
        RestTestClient ownerClient = RestTestClientAuth.withSession(restClient, venue.manager());
        RestTestClient employeeClient = RestTestClientAuth.withSession(restClient, employee);

        ownerClient.patch()
                .uri("/api/v1/venues/{venueId}/members/{memberId}", venue.venueId(), employeeMembershipId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("status", "inactive"))
                .exchange()
                .expectStatus()
                .isOk();

        // Act & Assert
        employeeClient.get()
                .uri("/api/v1/venues/{venueId}/me", venue.venueId())
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody()
                .jsonPath("$.status").isEqualTo(403)
                .jsonPath("$.message").isEqualTo("Your access to this venue has been blocked.")
                .jsonPath("$.errorCode").isEqualTo("MEMBERSHIP_INACTIVE");
    }

    @Test
    void getMembershipReturnsNotFoundWhenVenueDoesNotExist() {
        // Arrange
        AuthSession user = authSessionPolluter.registerPasswordUser();
        RestTestClient userClient = RestTestClientAuth.withSession(restClient, user);
        java.util.UUID missingVenueId = java.util.UUID.randomUUID();

        // Act & Assert
        userClient.get()
                .uri("/api/v1/venues/{venueId}/me", missingVenueId)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.errorCode").isEqualTo("NOT_FOUND");

        assertThat(venueRepository.findById(missingVenueId)).isEmpty();
    }

    @Test
    void getMembershipReturnsBadRequestWhenVenueIdIsMalformed() {
        // Arrange
        AuthSession user = authSessionPolluter.registerPasswordUser();
        RestTestClient userClient = RestTestClientAuth.withSession(restClient, user);

        // Act & Assert
        userClient.get()
                .uri("/api/v1/venues/not-a-uuid/me")
                .exchange()
                .expectStatus()
                .isBadRequest();
    }
}
