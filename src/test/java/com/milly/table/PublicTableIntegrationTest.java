package com.milly.table;

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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PublicTableIntegrationTest {

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

    @BeforeEach
    void setUp() {
        UserEntity manager = userRepository.save(UserEntity.createActive(
                "Manager", "User", "manager@example.com", LocalDate.of(1990, 1, 1)));

        VenueEntity venue = venueRepository.save(VenueEntity.createActive("Test Venue", "Baku"));
        venueId = venue.getId();

        venueMembershipRepository.save(VenueMembershipEntity.create(venueId, manager.getId(), VenueRole.MANAGER));

        managerAccessToken = jwtTokenService.issueAccessToken(new AuthUser(manager.getId(), List.of(RoleName.USER)));
    }

    @Test
    void activeTableReturnsTableDetailsWithoutAuth() throws Exception {
        UUID tableId = createTable("Table 5");

        mockMvc.perform(get("/api/v1/public/tables/{tableId}", tableId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(tableId.toString()))
                .andExpect(jsonPath("$.data.venueId").value(venueId.toString()))
                .andExpect(jsonPath("$.data.label").value("Table 5"))
                .andExpect(jsonPath("$.data.status").value(TableStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.data.createdAt").doesNotExist())
                .andExpect(jsonPath("$.data.updatedAt").doesNotExist());
    }

    @Test
    void unknownTableReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/public/tables/{tableId}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @Test
    void inactiveTableReturnsNotFound() throws Exception {
        UUID tableId = createTable("Table 1");

        mockMvc.perform(post("/api/v1/venues/{venueId}/tables/{tableId}/deactivate", venueId, tableId)
                        .cookie(accessTokenCookie(managerAccessToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/public/tables/{tableId}", tableId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    private UUID createTable(String label) throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/v1/venues/{venueId}/tables", venueId)
                        .cookie(accessTokenCookie(managerAccessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"" + label + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(createResult.getResponse().getContentAsString());
        JsonNode id = root.path("data").path("id");
        assertThat(id.isMissingNode()).isFalse();
        return UUID.fromString(id.asText());
    }

    private Cookie accessTokenCookie(String accessToken) {
        return new Cookie(AuthCookieWriter.ACCESS_TOKEN_COOKIE, accessToken);
    }
}
