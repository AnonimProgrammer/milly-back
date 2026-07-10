package com.milly.menu.infrastructure.adapter.inbound.http;

import com.milly.auth.application.polluter.AuthSession;
import com.milly.auth.infrastructure.adapter.outbound.security.AuthCookieWriter;
import com.milly.config.domain.AbstractITest;
import com.milly.config.infrastructure.adapter.RestTestClientAuth;
import com.milly.menu.application.polluter.MenuItemPolluter;
import com.milly.menu.domain.entity.MenuItemEntity;
import com.milly.menu.domain.valueobject.MenuItemStatus;
import com.milly.menu.infrastructure.adapter.inbound.http.dto.MenuItemListApiResponse;
import com.milly.menu.infrastructure.adapter.inbound.http.dto.MenuItemApiResponse;
import com.milly.menu.infrastructure.adapter.outbound.persistence.MenuItemJpaRepository;
import com.milly.table.domain.entity.TableEntity;
import com.milly.table.domain.valueobject.TableStatus;
import com.milly.table.infrastructure.adapter.outbound.persistence.TableJpaRepository;
import com.milly.venue.application.polluter.ManagedVenue;
import com.milly.venue.application.polluter.VenuePolluter;
import com.milly.venue.domain.valueobject.VenueRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MenuItemRestIntegrationTest extends AbstractITest {

    @Autowired
    private RestTestClient restClient;

    @Autowired
    private VenuePolluter venuePolluter;

    @Autowired
    private MenuItemPolluter menuItemPolluter;

    @Autowired
    private MenuItemJpaRepository menuItemRepository;

    @Autowired
    private TableJpaRepository tableRepository;

    @Test
    void managerListsOnlyActiveMenuItemsForVenue() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        ManagedVenue otherVenue = venuePolluter.createManagedVenue();
        MenuItemEntity burger = menuItemPolluter.createActiveItem(venue.venueId(), "Burger");
        MenuItemEntity pasta = menuItemPolluter.createActiveItem(venue.venueId(), "Pasta");
        menuItemPolluter.createDeletedItem(venue.venueId(), "Deleted");
        menuItemPolluter.createActiveItem(otherVenue.venueId(), "Other venue");
        RestTestClient managerClient = RestTestClientAuth.withSession(restClient, venue.manager());

        // Act
        MenuItemListApiResponse response = managerClient.get()
                .uri(menuItemsPath(venue.venueId()))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(MenuItemListApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getMessage()).isEqualTo("Menu items retrieved successfully.");
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getData()).hasSize(2);
        assertThat(response.getData()).extracting(item -> item.id()).containsExactly(burger.getId(), pasta.getId());
        assertThat(response.getData()).extracting(item -> item.venueId())
                .containsExactly(venue.venueId(), venue.venueId());
        assertThat(response.getData()).extracting(item -> item.status())
                .containsExactly(MenuItemStatus.ACTIVE, MenuItemStatus.ACTIVE);
    }

    @Test
    void unauthenticatedListMenuItemsReturnsUnauthorized() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();

        // Act & Assert
        restClient.get()
                .uri(menuItemsPath(venue.venueId()))
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.errorCode").isEqualTo("UNAUTHORIZED");
    }

    @Test
    void nonMemberListMenuItemsReturnsForbidden() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        ManagedVenue otherVenue = venuePolluter.createManagedVenue();
        AuthSession nonMember = venuePolluter.addMember(otherVenue.venueId(), VenueRole.MANAGER);
        RestTestClient nonMemberClient = RestTestClientAuth.withSession(restClient, nonMember);

        // Act & Assert
        nonMemberClient.get()
                .uri(menuItemsPath(venue.venueId()))
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody()
                .jsonPath("$.status").isEqualTo(403)
                .jsonPath("$.errorCode").isEqualTo("FORBIDDEN");
    }

    @Test
    void waiterListMenuItemsReturnsForbidden() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        AuthSession waiter = venuePolluter.addMember(venue.venueId(), VenueRole.WAITER);
        RestTestClient waiterClient = RestTestClientAuth.withSession(restClient, waiter);

        // Act & Assert
        waiterClient.get()
                .uri(menuItemsPath(venue.venueId()))
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody()
                .jsonPath("$.status").isEqualTo(403)
                .jsonPath("$.errorCode").isEqualTo("FORBIDDEN");
    }

    @Test
    void invalidSessionListMenuItemsReturnsUnauthorized() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        RestTestClient invalidClient = restClient.mutate()
                .defaultCookie(AuthCookieWriter.ACCESS_TOKEN_COOKIE, "not-a-valid-token")
                .build();

        // Act & Assert
        invalidClient.get()
                .uri(menuItemsPath(venue.venueId()))
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.errorCode").isEqualTo("UNAUTHORIZED");
    }

    @Test
    void managerCreatesMenuItem() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        RestTestClient managerClient = RestTestClientAuth.withSession(restClient, venue.manager());

        // Act
        MenuItemApiResponse response = managerClient.post()
                .uri(menuItemsPath(venue.venueId()))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"name":" Pizza ","description":" Cheese ","price":12.50,"approximatePreparationMinutes":20}
                        """)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(MenuItemApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(201);
        assertThat(response.getMessage()).isEqualTo("Menu item created successfully.");
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getData().id()).isNotNull();
        assertThat(response.getData().venueId()).isEqualTo(venue.venueId());
        assertThat(response.getData().name()).isEqualTo("Pizza");
        assertThat(response.getData().description()).isEqualTo("Cheese");
        assertThat(response.getData().price()).isEqualByComparingTo("12.50");
        assertThat(response.getData().approximatePreparationMinutes()).isEqualTo(20);
        assertThat(response.getData().status()).isEqualTo(MenuItemStatus.ACTIVE);
        assertThat(menuItemRepository.findById(response.getData().id()))
                .hasValueSatisfying(item -> {
                    assertThat(item.getVenueId()).isEqualTo(venue.venueId());
                    assertThat(item.getName()).isEqualTo("Pizza");
                    assertThat(item.getDescription()).isEqualTo("Cheese");
                    assertThat(item.getPrice().amount()).isEqualByComparingTo("12.50");
                    assertThat(item.getApproximatePreparationMinutes()).isEqualTo(20);
                    assertThat(item.getStatus()).isEqualTo(MenuItemStatus.ACTIVE);
                });
    }

    @Test
    void unauthenticatedCreateMenuItemReturnsUnauthorized() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        long itemCount = menuItemRepository.count();

        // Act & Assert
        restClient.post()
                .uri(menuItemsPath(venue.venueId()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(validCreateBody())
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.errorCode").isEqualTo("UNAUTHORIZED");

        assertThat(menuItemRepository.count()).isEqualTo(itemCount);
    }

    @Test
    void nonMemberCreateMenuItemReturnsForbidden() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        ManagedVenue otherVenue = venuePolluter.createManagedVenue();
        AuthSession nonMember = venuePolluter.addMember(otherVenue.venueId(), VenueRole.MANAGER);
        RestTestClient nonMemberClient = RestTestClientAuth.withSession(restClient, nonMember);
        long itemCount = menuItemRepository.count();

        // Act & Assert
        nonMemberClient.post()
                .uri(menuItemsPath(venue.venueId()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(validCreateBody())
                .exchange()
                .expectStatus()
                .isForbidden()
