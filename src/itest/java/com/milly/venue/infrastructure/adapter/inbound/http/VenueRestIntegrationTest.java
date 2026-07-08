package com.milly.venue.infrastructure.adapter.inbound.http;

import com.milly.auth.application.polluter.AuthSession;
import com.milly.auth.application.polluter.AuthSessionPolluter;
import com.milly.config.domain.AbstractITest;
import com.milly.config.infrastructure.adapter.RestTestClientAuth;
import com.milly.venue.domain.valueobject.VenueRole;
import com.milly.venue.domain.valueobject.VenueStatus;
import com.milly.venue.infrastructure.adapter.inbound.http.dto.CreateVenueApiResponse;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueJpaRepository;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueMembershipJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class VenueRestIntegrationTest extends AbstractITest {

    @Autowired
    private RestTestClient restClient;

    @Autowired
    private AuthSessionPolluter authSessionPolluter;

    @Autowired
    private VenueJpaRepository venueRepository;

    @Autowired
    private VenueMembershipJpaRepository venueMembershipRepository;

    @Test
    void authenticatedUserCreatesVenueAndManagerMembership() {
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
        assertThat(response.getData().role()).isEqualTo(VenueRole.MANAGER);

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
                    assertThat(membership.getRole()).isEqualTo(VenueRole.MANAGER);
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
}
