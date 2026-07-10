package com.milly.menu.infrastructure.adapter.inbound.http;

import com.milly.auth.application.polluter.AuthSession;
import com.milly.auth.infrastructure.adapter.outbound.security.AuthCookieWriter;
import com.milly.config.domain.AbstractITest;
import com.milly.config.infrastructure.adapter.RestTestClientAuth;
import com.milly.menu.application.polluter.MenuItemPolluter;
import com.milly.menu.domain.entity.MenuItemEntity;
import com.milly.menu.domain.valueobject.MenuItemCategory;
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
                        {"name":" Pizza ","description":" Cheese ","price":12.50,"approximatePreparationMinutes":20,"category":"Mains"}
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
        assertThat(response.getData().category()).isEqualTo(MenuItemCategory.MAINS);
        assertThat(response.getData().status()).isEqualTo(MenuItemStatus.ACTIVE);
        assertThat(menuItemRepository.findById(response.getData().id()))
                .hasValueSatisfying(item -> {
                    assertThat(item.getVenueId()).isEqualTo(venue.venueId());
                    assertThat(item.getName()).isEqualTo("Pizza");
                    assertThat(item.getDescription()).isEqualTo("Cheese");
                    assertThat(item.getPrice().amount()).isEqualByComparingTo("12.50");
                    assertThat(item.getApproximatePreparationMinutes()).isEqualTo(20);
                    assertThat(item.getCategory()).isEqualTo(MenuItemCategory.MAINS);
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
                .expectBody()
                .jsonPath("$.status").isEqualTo(403)
                .jsonPath("$.errorCode").isEqualTo("FORBIDDEN");

        assertThat(menuItemRepository.count()).isEqualTo(itemCount);
    }

    @Test
    void waiterCreateMenuItemReturnsForbidden() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        AuthSession waiter = venuePolluter.addMember(venue.venueId(), VenueRole.WAITER);
        RestTestClient waiterClient = RestTestClientAuth.withSession(restClient, waiter);
        long itemCount = menuItemRepository.count();

        // Act & Assert
        waiterClient.post()
                .uri(menuItemsPath(venue.venueId()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(validCreateBody())
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody()
                .jsonPath("$.status").isEqualTo(403)
                .jsonPath("$.errorCode").isEqualTo("FORBIDDEN");

        assertThat(menuItemRepository.count()).isEqualTo(itemCount);
    }

    @Test
    void createMenuItemReturnsBadRequestForBlankName() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        RestTestClient managerClient = managerClientFor(venue);
        long itemCount = menuItemRepository.count();

        // Act & Assert
        managerClient.post()
                .uri(menuItemsPath(venue.venueId()))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"name":" ","description":"Cheese","price":12.50,"approximatePreparationMinutes":15,"category":"Mains"}
                        """)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("Name is required.")
                .jsonPath("$.errorCode").isEqualTo("BAD_REQUEST");

        assertThat(menuItemRepository.count()).isEqualTo(itemCount);
    }

    @Test
    void createMenuItemReturnsBadRequestForMissingPrice() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        RestTestClient managerClient = managerClientFor(venue);
        long itemCount = menuItemRepository.count();

        // Act & Assert
        managerClient.post()
                .uri(menuItemsPath(venue.venueId()))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"name":"Pizza","description":"Cheese","approximatePreparationMinutes":15,"category":"Mains"}
                        """)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("Price is required.")
                .jsonPath("$.errorCode").isEqualTo("BAD_REQUEST");

        assertThat(menuItemRepository.count()).isEqualTo(itemCount);
    }

    @Test
    void createMenuItemReturnsBadRequestForMissingApproximatePreparationMinutes() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        RestTestClient managerClient = managerClientFor(venue);
        long itemCount = menuItemRepository.count();

        // Act & Assert
        managerClient.post()
                .uri(menuItemsPath(venue.venueId()))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"name":"Pizza","description":"Cheese","price":12.50,"category":"Mains"}
                        """)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("Approximate preparation time is required.")
                .jsonPath("$.errorCode").isEqualTo("BAD_REQUEST");

        assertThat(menuItemRepository.count()).isEqualTo(itemCount);
    }

    @Test
    void createMenuItemReturnsBadRequestForNegativePrice() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        RestTestClient managerClient = managerClientFor(venue);
        long itemCount = menuItemRepository.count();

        // Act & Assert
        managerClient.post()
                .uri(menuItemsPath(venue.venueId()))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"name":"Pizza","description":"Cheese","price":-1,"approximatePreparationMinutes":15,"category":"Mains"}
                        """)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("Price must be at least 0.01.")
                .jsonPath("$.errorCode").isEqualTo("BAD_REQUEST");

        assertThat(menuItemRepository.count()).isEqualTo(itemCount);
    }

    @Test
    void managerGetsMenuItemFromVenue() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        MenuItemEntity item = menuItemPolluter.createItem(
                venue.venueId(), "Pizza", "Cheese", "12.50", MenuItemStatus.ACTIVE);
        RestTestClient managerClient = managerClientFor(venue);

        // Act
        MenuItemApiResponse response = managerClient.get()
                .uri(menuItemPath(venue.venueId(), item.getId()))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(MenuItemApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getMessage()).isEqualTo("Menu item retrieved successfully.");
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getData().id()).isEqualTo(item.getId());
        assertThat(response.getData().venueId()).isEqualTo(venue.venueId());
        assertThat(response.getData().name()).isEqualTo("Pizza");
        assertThat(response.getData().description()).isEqualTo("Cheese");
        assertThat(response.getData().price()).isEqualByComparingTo("12.50");
        assertThat(response.getData().status()).isEqualTo(MenuItemStatus.ACTIVE);
    }

    @Test
    void unauthenticatedGetMenuItemReturnsUnauthorized() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        MenuItemEntity item = menuItemPolluter.createActiveItem(venue.venueId(), "Pizza");

        // Act & Assert
        restClient.get()
                .uri(menuItemPath(venue.venueId(), item.getId()))
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.errorCode").isEqualTo("UNAUTHORIZED");
    }

    @Test
    void nonMemberGetMenuItemReturnsForbidden() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        ManagedVenue otherVenue = venuePolluter.createManagedVenue();
        AuthSession nonMember = venuePolluter.addMember(otherVenue.venueId(), VenueRole.MANAGER);
        MenuItemEntity item = menuItemPolluter.createActiveItem(venue.venueId(), "Pizza");
        RestTestClient nonMemberClient = RestTestClientAuth.withSession(restClient, nonMember);

        // Act & Assert
        nonMemberClient.get()
                .uri(menuItemPath(venue.venueId(), item.getId()))
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody()
                .jsonPath("$.status").isEqualTo(403)
                .jsonPath("$.errorCode").isEqualTo("FORBIDDEN");
    }

    @Test
    void waiterGetMenuItemReturnsForbidden() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        AuthSession waiter = venuePolluter.addMember(venue.venueId(), VenueRole.WAITER);
        MenuItemEntity item = menuItemPolluter.createActiveItem(venue.venueId(), "Pizza");
        RestTestClient waiterClient = RestTestClientAuth.withSession(restClient, waiter);

        // Act & Assert
        waiterClient.get()
                .uri(menuItemPath(venue.venueId(), item.getId()))
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody()
                .jsonPath("$.status").isEqualTo(403)
                .jsonPath("$.errorCode").isEqualTo("FORBIDDEN");
    }

    @Test
    void getMenuItemReturnsNotFoundWhenItemDoesNotExist() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        RestTestClient managerClient = managerClientFor(venue);
        UUID missingItemId = UUID.randomUUID();

        // Act & Assert
        managerClient.get()
                .uri(menuItemPath(venue.venueId(), missingItemId))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.errorCode").isEqualTo("NOT_FOUND");
    }

    @Test
    void getMenuItemReturnsNotFoundWhenItemBelongsToAnotherVenue() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        ManagedVenue otherVenue = venuePolluter.createManagedVenue();
        MenuItemEntity otherItem = menuItemPolluter.createActiveItem(otherVenue.venueId(), "Pizza");
        RestTestClient managerClient = managerClientFor(venue);

        // Act & Assert
        managerClient.get()
                .uri(menuItemPath(venue.venueId(), otherItem.getId()))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.errorCode").isEqualTo("NOT_FOUND");
    }

    @Test
    void managerPartiallyUpdatesMenuItemPriceOnly() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        MenuItemEntity item = menuItemPolluter.createItem(
                venue.venueId(), "Pizza", "Cheese", "12.50", MenuItemStatus.ACTIVE);
        RestTestClient managerClient = managerClientFor(venue);

        // Act
        MenuItemApiResponse response = managerClient.patch()
                .uri(menuItemPath(venue.venueId(), item.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"price":14.25}
                        """)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(MenuItemApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getMessage()).isEqualTo("Menu item updated successfully.");
        assertThat(response.getData().id()).isEqualTo(item.getId());
        assertThat(response.getData().name()).isEqualTo("Pizza");
        assertThat(response.getData().description()).isEqualTo("Cheese");
        assertThat(response.getData().price()).isEqualByComparingTo("14.25");
        assertThat(response.getData().status()).isEqualTo(MenuItemStatus.ACTIVE);
        assertThat(menuItemRepository.findById(item.getId()))
                .hasValueSatisfying(stored -> {
                    assertThat(stored.getName()).isEqualTo("Pizza");
                    assertThat(stored.getDescription()).isEqualTo("Cheese");
                    assertThat(stored.getPrice().amount()).isEqualByComparingTo("14.25");
                    assertThat(stored.getStatus()).isEqualTo(MenuItemStatus.ACTIVE);
                });
    }

    @Test
    void unauthenticatedUpdateMenuItemReturnsUnauthorized() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        MenuItemEntity item = menuItemPolluter.createActiveItem(venue.venueId(), "Pizza");

        // Act & Assert
        restClient.patch()
                .uri(menuItemPath(venue.venueId(), item.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"price":14.25}
                        """)
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.errorCode").isEqualTo("UNAUTHORIZED");
    }

    @Test
    void nonMemberUpdateMenuItemReturnsForbidden() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        ManagedVenue otherVenue = venuePolluter.createManagedVenue();
        AuthSession nonMember = venuePolluter.addMember(otherVenue.venueId(), VenueRole.MANAGER);
        MenuItemEntity item = menuItemPolluter.createActiveItem(venue.venueId(), "Pizza");
        RestTestClient nonMemberClient = RestTestClientAuth.withSession(restClient, nonMember);

        // Act & Assert
        nonMemberClient.patch()
                .uri(menuItemPath(venue.venueId(), item.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"price":14.25}
                        """)
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody()
                .jsonPath("$.status").isEqualTo(403)
                .jsonPath("$.errorCode").isEqualTo("FORBIDDEN");
    }

    @Test
    void waiterUpdateMenuItemReturnsForbidden() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        AuthSession waiter = venuePolluter.addMember(venue.venueId(), VenueRole.WAITER);
        MenuItemEntity item = menuItemPolluter.createActiveItem(venue.venueId(), "Pizza");
        RestTestClient waiterClient = RestTestClientAuth.withSession(restClient, waiter);

        // Act & Assert
        waiterClient.patch()
                .uri(menuItemPath(venue.venueId(), item.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"price":14.25}
                        """)
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody()
                .jsonPath("$.status").isEqualTo(403)
                .jsonPath("$.errorCode").isEqualTo("FORBIDDEN");
    }

    @Test
    void updateMenuItemReturnsBadRequestForBlankName() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        MenuItemEntity item = menuItemPolluter.createActiveItem(venue.venueId(), "Pizza");
        RestTestClient managerClient = managerClientFor(venue);

        // Act & Assert
        managerClient.patch()
                .uri(menuItemPath(venue.venueId(), item.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"name":" "}
                        """)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("Name must not be blank.")
                .jsonPath("$.errorCode").isEqualTo("BAD_REQUEST");

        assertThat(menuItemRepository.findById(item.getId()))
                .hasValueSatisfying(stored -> assertThat(stored.getName()).isEqualTo("Pizza"));
    }

    @Test
    void updateMenuItemReturnsBadRequestForNegativePrice() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        MenuItemEntity item = menuItemPolluter.createActiveItem(venue.venueId(), "Pizza");
        RestTestClient managerClient = managerClientFor(venue);

        // Act & Assert
        managerClient.patch()
                .uri(menuItemPath(venue.venueId(), item.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"price":-1}
                        """)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("Price must be at least 0.01.")
                .jsonPath("$.errorCode").isEqualTo("BAD_REQUEST");
    }

    @Test
    void updateMenuItemReturnsNotFoundWhenItemDoesNotExist() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        RestTestClient managerClient = managerClientFor(venue);

        // Act & Assert
        managerClient.patch()
                .uri(menuItemPath(venue.venueId(), UUID.randomUUID()))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"price":14.25}
                        """)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.errorCode").isEqualTo("NOT_FOUND");
    }

    @Test
    void updateMenuItemReturnsNotFoundWhenItemBelongsToAnotherVenue() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        ManagedVenue otherVenue = venuePolluter.createManagedVenue();
        MenuItemEntity otherItem = menuItemPolluter.createActiveItem(otherVenue.venueId(), "Pizza");
        RestTestClient managerClient = managerClientFor(venue);

        // Act & Assert
        managerClient.patch()
                .uri(menuItemPath(venue.venueId(), otherItem.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"price":14.25}
                        """)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.errorCode").isEqualTo("NOT_FOUND");
    }

    @Test
    void managerDeletesMenuItem() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        MenuItemEntity item = menuItemPolluter.createActiveItem(venue.venueId(), "Pizza");
        RestTestClient managerClient = managerClientFor(venue);

        // Act & Assert
        managerClient.delete()
                .uri(menuItemPath(venue.venueId(), item.getId()))
                .exchange()
                .expectStatus()
                .isNoContent()
                .expectBody()
                .isEmpty();

        assertThat(menuItemRepository.findById(item.getId()))
                .hasValueSatisfying(stored -> assertThat(stored.getStatus()).isEqualTo(MenuItemStatus.DELETED));

        MenuItemListApiResponse response = managerClient.get()
                .uri(menuItemsPath(venue.venueId()))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(MenuItemListApiResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.getData()).extracting(menuItem -> menuItem.id()).doesNotContain(item.getId());
    }

    @Test
    void unauthenticatedDeleteMenuItemReturnsUnauthorized() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        MenuItemEntity item = menuItemPolluter.createActiveItem(venue.venueId(), "Pizza");

        // Act & Assert
        restClient.delete()
                .uri(menuItemPath(venue.venueId(), item.getId()))
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.errorCode").isEqualTo("UNAUTHORIZED");

        assertThat(menuItemRepository.findById(item.getId()))
                .hasValueSatisfying(stored -> assertThat(stored.getStatus()).isEqualTo(MenuItemStatus.ACTIVE));
    }

    @Test
    void nonMemberDeleteMenuItemReturnsForbidden() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        ManagedVenue otherVenue = venuePolluter.createManagedVenue();
        AuthSession nonMember = venuePolluter.addMember(otherVenue.venueId(), VenueRole.MANAGER);
        MenuItemEntity item = menuItemPolluter.createActiveItem(venue.venueId(), "Pizza");
        RestTestClient nonMemberClient = RestTestClientAuth.withSession(restClient, nonMember);

        // Act & Assert
        nonMemberClient.delete()
                .uri(menuItemPath(venue.venueId(), item.getId()))
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody()
                .jsonPath("$.status").isEqualTo(403)
                .jsonPath("$.errorCode").isEqualTo("FORBIDDEN");

        assertThat(menuItemRepository.findById(item.getId()))
                .hasValueSatisfying(stored -> assertThat(stored.getStatus()).isEqualTo(MenuItemStatus.ACTIVE));
    }

    @Test
    void waiterDeleteMenuItemReturnsForbidden() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        AuthSession waiter = venuePolluter.addMember(venue.venueId(), VenueRole.WAITER);
        MenuItemEntity item = menuItemPolluter.createActiveItem(venue.venueId(), "Pizza");
        RestTestClient waiterClient = RestTestClientAuth.withSession(restClient, waiter);

        // Act & Assert
        waiterClient.delete()
                .uri(menuItemPath(venue.venueId(), item.getId()))
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody()
                .jsonPath("$.status").isEqualTo(403)
                .jsonPath("$.errorCode").isEqualTo("FORBIDDEN");

        assertThat(menuItemRepository.findById(item.getId()))
                .hasValueSatisfying(stored -> assertThat(stored.getStatus()).isEqualTo(MenuItemStatus.ACTIVE));
    }

    @Test
    void deleteMenuItemReturnsNotFoundWhenItemDoesNotExist() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        RestTestClient managerClient = managerClientFor(venue);

        // Act & Assert
        managerClient.delete()
                .uri(menuItemPath(venue.venueId(), UUID.randomUUID()))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.errorCode").isEqualTo("NOT_FOUND");
    }

    @Test
    void deleteMenuItemReturnsNotFoundWhenItemBelongsToAnotherVenue() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        ManagedVenue otherVenue = venuePolluter.createManagedVenue();
        MenuItemEntity otherItem = menuItemPolluter.createActiveItem(otherVenue.venueId(), "Pizza");
        RestTestClient managerClient = managerClientFor(venue);

        // Act & Assert
        managerClient.delete()
                .uri(menuItemPath(venue.venueId(), otherItem.getId()))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.errorCode").isEqualTo("NOT_FOUND");

        assertThat(menuItemRepository.findById(otherItem.getId()))
                .hasValueSatisfying(stored -> assertThat(stored.getStatus()).isEqualTo(MenuItemStatus.ACTIVE));
    }

    @Test
    void publicMenuListsOnlyActiveMenuItemsForActiveTableVenue() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        ManagedVenue otherVenue = venuePolluter.createManagedVenue();
        TableEntity table = tableRepository.save(TableEntity.create(venue.venueId(), "Patio 1", TableStatus.ACTIVE));
        MenuItemEntity burger = menuItemPolluter.createItem(
                venue.venueId(), "Burger", "Beef", "15.00", MenuItemStatus.ACTIVE);
        MenuItemEntity pasta = menuItemPolluter.createItem(
                venue.venueId(), "Pasta", "Tomato", "11.50", MenuItemStatus.ACTIVE);
        menuItemPolluter.createDeletedItem(venue.venueId(), "Deleted");
        menuItemPolluter.createActiveItem(otherVenue.venueId(), "Other venue");

        // Act
        MenuItemListApiResponse response = restClient.get()
                .uri(publicMenuPath(table.getId()))
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
    void publicMenuReturnsNotFoundWhenTableDoesNotExist() {
        // Act & Assert
        restClient.get()
                .uri(publicMenuPath(UUID.randomUUID()))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.errorCode").isEqualTo("NOT_FOUND");
    }

    @Test
    void publicMenuReturnsNotFoundWhenTableIsInactive() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        TableEntity table = tableRepository.save(TableEntity.create(venue.venueId(), "Patio 1", TableStatus.INACTIVE));
        menuItemPolluter.createActiveItem(venue.venueId(), "Pizza");

        // Act & Assert
        restClient.get()
                .uri(publicMenuPath(table.getId()))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.errorCode").isEqualTo("NOT_FOUND");
    }

    @Test
    void publicMenuReturnsBadRequestWhenTableIdIsMalformed() {
        // Act & Assert
        restClient.get()
                .uri("/api/v1/public/tables/not-a-uuid/menu")
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("BAD_REQUEST");
    }

    private String menuItemsPath(UUID venueId) {
        return "/api/v1/venues/" + venueId + "/menu/items";
    }

    private String menuItemPath(UUID venueId, UUID itemId) {
        return menuItemsPath(venueId) + "/" + itemId;
    }

    private String publicMenuPath(UUID tableId) {
        return "/api/v1/public/tables/" + tableId + "/menu";
    }

    private RestTestClient managerClientFor(ManagedVenue venue) {
        return RestTestClientAuth.withSession(restClient, venue.manager());
    }

    private String validCreateBody() {
        return """
                {"name":"Pizza","description":"Cheese","price":12.50,"approximatePreparationMinutes":15,"category":"Mains"}
                """;
    }
}