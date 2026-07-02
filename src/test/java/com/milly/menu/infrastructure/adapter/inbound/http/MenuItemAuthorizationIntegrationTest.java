package com.milly.menu.infrastructure.adapter.inbound.http;

import com.milly.common.domain.valueobject.Money;
import com.milly.menu.domain.entity.MenuItemEntity;
import com.milly.menu.domain.valueobject.MenuItemStatus;
import com.milly.menu.infrastructure.adapter.outbound.persistence.MenuItemJpaRepository;
import com.milly.venue.domain.entity.VenueMembershipEntity;
import com.milly.venue.domain.valueobject.VenueRole;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueMembershipJpaRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MenuItemAuthorizationIntegrationTest {

    private static final String VALID_BODY = """
            {"name":"Pizza","description":"Cheese","price":12.50}
            """;
    private static final String PATCH_BODY = """
            {"name":"Pasta"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MenuItemJpaRepository menuItemRepository;

    @Autowired
    private VenueMembershipJpaRepository venueMembershipRepository;

    private UUID venueId;
    private UUID itemId;

    @BeforeEach
    void setUp() {
        venueId = UUID.randomUUID();
        itemId = menuItemRepository.save(MenuItemEntity.create(
                venueId, "Pizza", "Cheese", Money.of("12.50"), MenuItemStatus.ACTIVE)).getId();
    }

    @ParameterizedTest
    @EnumSource(Operation.class)
    void missingSessionReturnsUnauthorizedForEveryEndpoint(Operation operation) throws Exception {
        mockMvc.perform(request(operation, itemId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidSessionReturnsUnauthorized() throws Exception {
        mockMvc.perform(get(basePath()).cookie(new Cookie("access_token", "invalid")))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @EnumSource(Operation.class)
    void nonMemberReturnsForbiddenForEveryEndpoint(Operation operation) throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(authenticated(request(operation, itemId), userId))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @EnumSource(Operation.class)
    void waiterReturnsForbiddenForEveryEndpoint(Operation operation) throws Exception {
        UUID userId = UUID.randomUUID();
        venueMembershipRepository.save(VenueMembershipEntity.create(venueId, userId, VenueRole.WAITER));

        mockMvc.perform(authenticated(request(operation, itemId), userId))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @EnumSource(Operation.class)
    void managerIsAllowedForEveryEndpoint(Operation operation) throws Exception {
        UUID userId = UUID.randomUUID();
        venueMembershipRepository.save(VenueMembershipEntity.create(venueId, userId, VenueRole.MANAGER));

        mockMvc.perform(authenticated(request(operation, itemId), userId))
                .andExpect(status().is(operation.successStatus));
    }

    private MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder request, UUID userId) {
        return request.with(authentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of())));
    }

    private MockHttpServletRequestBuilder request(Operation operation, UUID targetItemId) {
        return switch (operation) {
            case LIST -> get(basePath());
            case CREATE -> post(basePath()).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY);
            case GET -> get(itemPath(targetItemId));
            case UPDATE -> patch(itemPath(targetItemId)).contentType(MediaType.APPLICATION_JSON).content(PATCH_BODY);
            case DELETE -> delete(itemPath(targetItemId));
        };
    }

    private String basePath() {
        return "/api/v1/venues/" + venueId + "/menu/items";
    }

    private String itemPath(UUID targetItemId) {
        return basePath() + "/" + targetItemId;
    }

    private enum Operation {
        LIST(200),
        CREATE(201),
        GET(200),
        UPDATE(200),
        DELETE(204);

        private final int successStatus;

        Operation(int successStatus) {
            this.successStatus = successStatus;
        }
    }
}
