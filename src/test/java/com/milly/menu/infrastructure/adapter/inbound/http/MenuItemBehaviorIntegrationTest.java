package com.milly.menu.infrastructure.adapter.inbound.http;

import com.milly.common.domain.valueobject.Money;
import com.milly.menu.domain.entity.MenuItemEntity;
import com.milly.menu.domain.valueobject.MenuItemStatus;
import com.milly.menu.infrastructure.adapter.outbound.persistence.MenuItemJpaRepository;
import com.milly.venue.domain.entity.VenueMembershipEntity;
import com.milly.venue.domain.valueobject.VenueRole;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueMembershipJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MenuItemBehaviorIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MenuItemJpaRepository menuItemRepository;

    @Autowired
    private VenueMembershipJpaRepository venueMembershipRepository;

    private UUID managerId;
    private UUID venueId;

    @BeforeEach
    void setUp() {
        managerId = UUID.randomUUID();
        venueId = UUID.randomUUID();
        venueMembershipRepository.save(VenueMembershipEntity.create(venueId, managerId, VenueRole.MANAGER));
    }

    @Test
    void listReturnsOnlyActiveItemsForVenue() throws Exception {
        saveItem(venueId, "Burger", MenuItemStatus.ACTIVE);
        saveItem(venueId, "Deleted", MenuItemStatus.DELETED);
        saveItem(UUID.randomUUID(), "Other venue", MenuItemStatus.ACTIVE);

        mockMvc.perform(authenticated(get(basePath())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Burger"))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"));
    }

    @Test
    void createReturnsCreatedActiveItem() throws Exception {
        mockMvc.perform(authenticated(post(basePath())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":" Pizza ","description":" Cheese ","price":12.50,"approximatePreparationMinutes":15}
                                """)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Pizza"))
                .andExpect(jsonPath("$.data.description").value("Cheese"))
                .andExpect(jsonPath("$.data.price").value(12.50))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void createRejectsBlankNameAndNonPositivePrice() throws Exception {
        mockMvc.perform(authenticated(post(basePath())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"  ","price":0,"approximatePreparationMinutes":15}
                                """)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(authenticated(post(basePath())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Pizza","price":-1,"approximatePreparationMinutes":15}
                                """)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getReturnsActiveItemAndHidesDeletedItem() throws Exception {
        MenuItemEntity active = saveItem(venueId, "Pizza", MenuItemStatus.ACTIVE);
        MenuItemEntity deleted = saveItem(venueId, "Old", MenuItemStatus.DELETED);

        mockMvc.perform(authenticated(get(itemPath(active.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(active.getId().toString()));

        mockMvc.perform(authenticated(get(itemPath(deleted.getId()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void itemFromAnotherVenueReturnsNotFound() throws Exception {
        MenuItemEntity otherVenueItem = saveItem(UUID.randomUUID(), "Pizza", MenuItemStatus.ACTIVE);

        mockMvc.perform(authenticated(get(itemPath(otherVenueItem.getId()))))
                .andExpect(status().isNotFound());

        mockMvc.perform(authenticated(patch(itemPath(otherVenueItem.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Pasta\"}")))
                .andExpect(status().isNotFound());

        mockMvc.perform(authenticated(delete(itemPath(otherVenueItem.getId()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void patchChangesOnlyProvidedFieldsAndValidatesValues() throws Exception {
        MenuItemEntity item = saveItem(venueId, "Pizza", MenuItemStatus.ACTIVE);

        mockMvc.perform(authenticated(patch(itemPath(item.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" Pasta \"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Pasta"))
                .andExpect(jsonPath("$.data.description").value("Description"))
                .andExpect(jsonPath("$.data.price").value(12.50));

        mockMvc.perform(authenticated(patch(itemPath(item.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  \",\"price\":0}")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteKeepsRowButHidesItemFromReads() throws Exception {
        MenuItemEntity item = saveItem(venueId, "Pizza", MenuItemStatus.ACTIVE);

        mockMvc.perform(authenticated(delete(itemPath(item.getId()))))
                .andExpect(status().isNoContent());

        MenuItemEntity stored = menuItemRepository.findById(item.getId()).orElseThrow();
        assertEquals(MenuItemStatus.DELETED, stored.getStatus());
        assertTrue(menuItemRepository.findByVenueIdAndStatusOrderByNameAsc(venueId, MenuItemStatus.ACTIVE).isEmpty());

        mockMvc.perform(authenticated(get(itemPath(item.getId()))))
                .andExpect(status().isNotFound());
    }

    private MenuItemEntity saveItem(UUID targetVenueId, String name, MenuItemStatus status) {
        return menuItemRepository.save(MenuItemEntity.create(
                targetVenueId, name, "Description", Money.of("12.50"), 15, status));
    }

    private MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder request) {
        return request.with(authentication(
                new UsernamePasswordAuthenticationToken(managerId, null, List.of())));
    }

    private String basePath() {
        return "/api/v1/venues/" + venueId + "/menu/items";
    }

    private String itemPath(UUID itemId) {
        return basePath() + "/" + itemId;
    }
}
