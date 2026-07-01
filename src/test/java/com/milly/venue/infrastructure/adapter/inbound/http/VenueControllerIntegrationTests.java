package com.milly.venue.infrastructure.adapter.inbound.http;

import com.milly.auth.domain.model.AuthUser;
import com.milly.auth.domain.valueobject.RoleName;
import com.milly.auth.infrastructure.adapter.outbound.security.AuthCookieWriter;
import com.milly.auth.infrastructure.adapter.outbound.security.JwtTokenService;
import com.milly.venue.domain.entity.VenueEntity;
import com.milly.venue.domain.entity.VenueMembershipEntity;
import com.milly.venue.domain.valueobject.VenueRole;
import com.milly.venue.domain.valueobject.VenueStatus;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueJpaRepository;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueMembershipJpaRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class VenueControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private VenueJpaRepository venueRepository;

    @Autowired
    private VenueMembershipJpaRepository venueMembershipRepository;

    @BeforeEach
    void cleanDatabase() {
        venueMembershipRepository.deleteAll();
        venueRepository.deleteAll();
    }

    @Test
    void createVenueReturnsCreatedVenueAndPersistsManagerMembership() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/venues")
                        .cookie(accessTokenCookie(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "  Milly Bistro  ",
                                  "location": "  Barcelona, Spain  "
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("Venue created successfully."))
                .andExpect(jsonPath("$.data.*", hasSize(4)))
                .andExpect(jsonPath("$.data.id").isString())
                .andExpect(jsonPath("$.data.name").value("Milly Bistro"))
                .andExpect(jsonPath("$.data.location").value("Barcelona, Spain"))
                .andExpect(jsonPath("$.data.role").value("MANAGER"));

        List<VenueEntity> venues = venueRepository.findAll();
        assertThat(venues).singleElement().satisfies(venue -> {
            assertThat(venue.getName()).isEqualTo("Milly Bistro");
            assertThat(venue.getLocation()).isEqualTo("Barcelona, Spain");
            assertThat(venue.getStatus()).isEqualTo(VenueStatus.ACTIVE);
        });

        VenueEntity venue = venues.getFirst();
        assertThat(venueMembershipRepository.findAll())
                .singleElement()
                .satisfies(membership -> assertManagerMembership(membership, venue.getId(), userId));
    }

    @Test
    void createVenueRejectsMissingFields() throws Exception {
        mockMvc.perform(post("/api/v1/venues")
                        .cookie(accessTokenCookie(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));

        assertNothingPersisted();
    }

    @Test
    void createVenueRejectsValuesBlankAfterTrimming() throws Exception {
        mockMvc.perform(post("/api/v1/venues")
                        .cookie(accessTokenCookie(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "   ",
                                  "location": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));

        assertNothingPersisted();
    }

    @Test
    void createVenueRejectsNameLongerThanStandardVarcharLimit() throws Exception {
        String request = """
                {
                  "name": "%s",
                  "location": "Barcelona, Spain"
                }
                """.formatted("a".repeat(256));

        mockMvc.perform(post("/api/v1/venues")
                        .cookie(accessTokenCookie(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Name must be at most 255 characters."))
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));

        assertNothingPersisted();
    }

    @Test
    void createVenueReturnsUnauthorizedWithoutSessionCookie() throws Exception {
        mockMvc.perform(post("/api/v1/venues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        assertNothingPersisted();
    }

    @Test
    void createVenueReturnsUnauthorizedForInvalidSessionCookie() throws Exception {
        mockMvc.perform(post("/api/v1/venues")
                        .cookie(new Cookie(AuthCookieWriter.ACCESS_TOKEN_COOKIE, "invalid-token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        assertNothingPersisted();
    }

    @Test
    void getVenueMembershipReturnsActualMembershipRole() throws Exception {
        UUID userId = UUID.randomUUID();
        VenueEntity venue = venueRepository.save(
                VenueEntity.createActive("Milly Bistro", "Barcelona, Spain"));
        venueMembershipRepository.save(
                VenueMembershipEntity.create(venue.getId(), userId, VenueRole.WAITER));

        mockMvc.perform(get("/api/v1/venues/{id}/me", venue.getId())
                        .cookie(accessTokenCookie(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Venue membership retrieved successfully."))
                .andExpect(jsonPath("$.data.*", hasSize(4)))
                .andExpect(jsonPath("$.data.venueId").value(venue.getId().toString()))
                .andExpect(jsonPath("$.data.venueName").value("Milly Bistro"))
                .andExpect(jsonPath("$.data.location").value("Barcelona, Spain"))
                .andExpect(jsonPath("$.data.role").value("WAITER"));
    }

    @Test
    void getVenueMembershipReturnsForbiddenWithoutMembership() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/venues/{id}/me", venueId)
                        .cookie(accessTokenCookie(userId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("You do not have access to this venue."))
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));

        assertNothingPersisted();
    }

    @Test
    void getVenueMembershipReturnsUnauthorizedWithoutSessionCookie() throws Exception {
        mockMvc.perform(get("/api/v1/venues/{id}/me", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        assertNothingPersisted();
    }

    private Cookie accessTokenCookie(UUID userId) {
        String accessToken = jwtTokenService.issueAccessToken(new AuthUser(userId, List.of(RoleName.USER)));
        return new Cookie(AuthCookieWriter.ACCESS_TOKEN_COOKIE, accessToken);
    }

    private void assertManagerMembership(VenueMembershipEntity membership, UUID venueId, UUID userId) {
        assertThat(membership.getVenueId()).isEqualTo(venueId);
        assertThat(membership.getUserId()).isEqualTo(userId);
        assertThat(membership.getRole()).isEqualTo(VenueRole.MANAGER);
    }

    private void assertNothingPersisted() {
        assertThat(venueRepository.count()).isZero();
        assertThat(venueMembershipRepository.count()).isZero();
    }

    private String validRequest() {
        return """
                {
                  "name": "Milly Bistro",
                  "location": "Barcelona, Spain"
                }
                """;
    }
}
