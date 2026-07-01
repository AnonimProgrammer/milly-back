package com.milly.table;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.milly.auth.domain.entity.UserEntity;
import com.milly.auth.domain.model.AuthUser;
import com.milly.auth.domain.valueobject.RoleName;
import com.milly.auth.infrastructure.adapter.outbound.persistence.UserJpaRepository;
import com.milly.auth.infrastructure.adapter.outbound.security.AuthCookieWriter;
import com.milly.auth.infrastructure.adapter.outbound.security.JwtTokenService;
import com.milly.table.domain.valueobject.TableStatus;
import com.milly.venue.domain.entity.VenueEntity;
import com.milly.venue.domain.entity.VenueMembershipEntity;
import com.milly.venue.domain.valueobject.VenueRole;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueJpaRepository;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueMembershipJpaRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TableCrudIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private VenueJpaRepository venueRepository;

    @Autowired
    private VenueMembershipJpaRepository venueMembershipRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    private UUID venueId;
    private String managerAccessToken;
    private String waiterAccessToken;

    @BeforeEach
    void setUp() {
        UserEntity manager = userRepository.save(UserEntity.createActive(
                "Manager", "User", "manager@example.com", LocalDate.of(1990, 1, 1)));
        UserEntity waiter = userRepository.save(UserEntity.createActive(
                "Waiter", "User", "waiter@example.com", LocalDate.of(1990, 1, 1)));

        VenueEntity venue = venueRepository.save(VenueEntity.createActive("Test Venue", "Baku"));
        venueId = venue.getId();

        venueMembershipRepository.save(VenueMembershipEntity.create(venueId, manager.getId(), VenueRole.MANAGER));
        venueMembershipRepository.save(VenueMembershipEntity.create(venueId, waiter.getId(), VenueRole.WAITER));

        managerAccessToken = jwtTokenService.issueAccessToken(new AuthUser(manager.getId(), List.of(RoleName.USER)));
        waiterAccessToken = jwtTokenService.issueAccessToken(new AuthUser(waiter.getId(), List.of(RoleName.USER)));
    }

    @Test
    void managerCanCreateListUpdateAndDeactivateTable() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/v1/venues/{venueId}/tables", venueId)
                        .cookie(accessTokenCookie(managerAccessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"Table 1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.label").value("Table 1"))
                .andExpect(jsonPath("$.data.status").value(TableStatus.ACTIVE.name()))
                .andReturn();

        UUID tableId = UUID.fromString(readDataField(createResult, "id"));

        mockMvc.perform(get("/api/v1/venues/{venueId}/tables", venueId)
                        .cookie(accessTokenCookie(managerAccessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(tableId.toString()))
                .andExpect(jsonPath("$.data[0].status").value(TableStatus.ACTIVE.name()));

        mockMvc.perform(get("/api/v1/venues/{venueId}/tables/{tableId}", venueId, tableId)
                        .cookie(accessTokenCookie(managerAccessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.label").value("Table 1"));

        mockMvc.perform(patch("/api/v1/venues/{venueId}/tables/{tableId}", venueId, tableId)
                        .cookie(accessTokenCookie(managerAccessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"Table A\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.label").value("Table A"));

        mockMvc.perform(post("/api/v1/venues/{venueId}/tables/{tableId}/deactivate", venueId, tableId)
                        .cookie(accessTokenCookie(managerAccessToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/venues/{venueId}/tables", venueId)
                        .cookie(accessTokenCookie(managerAccessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value(TableStatus.INACTIVE.name()));
    }

    @Test
    void waiterGetsForbiddenOnTableRoutes() throws Exception {
        mockMvc.perform(get("/api/v1/venues/{venueId}/tables", venueId)
                        .cookie(accessTokenCookie(waiterAccessToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));

        mockMvc.perform(post("/api/v1/venues/{venueId}/tables", venueId)
                        .cookie(accessTokenCookie(waiterAccessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"Table 1\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void unauthenticatedRequestGetsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/venues/{venueId}/tables", venueId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    private Cookie accessTokenCookie(String accessToken) {
        return new Cookie(AuthCookieWriter.ACCESS_TOKEN_COOKIE, accessToken);
    }

    private String readDataField(MvcResult result, String fieldName) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode value = root.path("data").path(fieldName);
        assertThat(value.isMissingNode()).isFalse();
        return value.asText();
    }
}
