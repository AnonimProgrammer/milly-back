package com.milly.table.infrastructure.adapter.inbound.http;

import com.milly.table.domain.entity.TableEntity;
import com.milly.table.domain.valueobject.TableStatus;
import com.milly.table.infrastructure.adapter.outbound.persistence.TableJpaRepository;
import com.milly.venue.domain.entity.VenueMembershipEntity;
import com.milly.venue.domain.valueobject.VenueRole;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueMembershipJpaRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TableQrAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TableJpaRepository tableRepository;

    @Autowired
    private VenueMembershipJpaRepository venueMembershipRepository;

    private UUID venueId;
    private UUID activeTableId;
    private UUID inactiveTableId;

    @BeforeEach
    void setUp() {
        venueId = UUID.randomUUID();
        activeTableId = tableRepository.save(TableEntity.create(venueId, "Table 1", TableStatus.ACTIVE)).getId();
        inactiveTableId = tableRepository.save(TableEntity.create(venueId, "Table 2", TableStatus.INACTIVE)).getId();
    }

    @Test
    void missingSessionReturnsUnauthorized() throws Exception {
        mockMvc.perform(post(qrPath(activeTableId)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidSessionReturnsUnauthorized() throws Exception {
        mockMvc.perform(post(qrPath(activeTableId)).cookie(new Cookie("access_token", "invalid")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonMemberReturnsForbidden() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post(qrPath(activeTableId)).with(authentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void waiterReturnsForbidden() throws Exception {
        UUID userId = UUID.randomUUID();
        venueMembershipRepository.save(VenueMembershipEntity.create(venueId, userId, VenueRole.WAITER));

        mockMvc.perform(post(qrPath(activeTableId)).with(authentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerReceivesQrForActiveTable() throws Exception {
        UUID userId = UUID.randomUUID();
        venueMembershipRepository.save(VenueMembershipEntity.create(venueId, userId, VenueRole.MANAGER));

        mockMvc.perform(post(qrPath(activeTableId)).with(authentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tableId").value(activeTableId.toString()))
                .andExpect(jsonPath("$.data.customerUrl").value("http://localhost:3000/table/" + activeTableId))
                .andExpect(jsonPath("$.data.qrImageUrl").value(
                        "https://storage.local/venues/" + venueId + "/tables/" + activeTableId + "/qr.png"));
    }

    @Test
    void managerReceivesNotFoundForInactiveTable() throws Exception {
        UUID userId = UUID.randomUUID();
        venueMembershipRepository.save(VenueMembershipEntity.create(venueId, userId, VenueRole.MANAGER));

        mockMvc.perform(post(qrPath(inactiveTableId)).with(authentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void managerReceivesNotFoundForUnknownTable() throws Exception {
        UUID userId = UUID.randomUUID();
        venueMembershipRepository.save(VenueMembershipEntity.create(venueId, userId, VenueRole.MANAGER));

        mockMvc.perform(post(qrPath(UUID.randomUUID())).with(authentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void managerReceivesNotFoundWhenTableBelongsToAnotherVenue() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID otherVenueId = UUID.randomUUID();
        venueMembershipRepository.save(VenueMembershipEntity.create(otherVenueId, userId, VenueRole.MANAGER));

        mockMvc.perform(post("/api/v1/venues/" + otherVenueId + "/tables/" + activeTableId + "/qr")
                        .with(authentication(new UsernamePasswordAuthenticationToken(userId, null, List.of()))))
                .andExpect(status().isNotFound());
    }

    private String qrPath(UUID tableId) {
        return "/api/v1/venues/" + venueId + "/tables/" + tableId + "/qr";
    }
}
