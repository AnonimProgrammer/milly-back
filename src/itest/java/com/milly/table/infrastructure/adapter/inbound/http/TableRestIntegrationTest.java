package com.milly.table.infrastructure.adapter.inbound.http;

import com.milly.config.domain.AbstractITest;
import com.milly.config.infrastructure.adapter.RestTestClientAuth;
import com.milly.table.application.polluter.TablePolluter;
import com.milly.table.application.polluter.TableTestFixture;
import com.milly.table.application.dto.TableResponse;
import com.milly.table.application.dto.TableQrResponse;
import com.milly.table.domain.valueobject.TableStatus;
import com.milly.table.infrastructure.adapter.inbound.http.dto.TableApiResponse;
import com.milly.table.infrastructure.adapter.inbound.http.dto.TableListApiResponse;
import com.milly.table.infrastructure.adapter.inbound.http.dto.TableQrApiResponse;
import com.milly.table.infrastructure.adapter.outbound.persistence.TableJpaRepository;
import com.milly.auth.application.polluter.AuthSession;
import com.milly.venue.application.polluter.ManagedVenue;
import com.milly.venue.application.polluter.VenuePolluter;
import com.milly.venue.domain.valueobject.VenueRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TableRestIntegrationTest extends AbstractITest {

    @Autowired
    private RestTestClient restClient;

    @Autowired
    private VenuePolluter venuePolluter;

    @Autowired
    private TablePolluter tablePolluter;

    @Autowired
    private TableJpaRepository tableRepository;

    private RestTestClient managerClientFor(ManagedVenue venue) {
        return RestTestClientAuth.withSession(restClient, venue.manager());
    }

    private static String tablesPath(UUID venueId) {
        return "/api/v1/venues/" + venueId + "/tables";
    }

    private String tablePath(UUID venueId, UUID tableId) {
        return tablesPath(venueId) + "/" + tableId;
    }

    private String deactivateTablePath(UUID venueId, UUID tableId) {
        return tablePath(venueId, tableId) + "/deactivate";
    }

    private String qrPath(UUID venueId, UUID tableId) {
        return tablePath(venueId, tableId) + "/qr";
    }

    @Test
    void unauthenticatedListTablesReturnsUnauthorized() {
        // Act & Assert
        restClient.get()
                .uri(tablesPath(UUID.randomUUID()))
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    @Test
    void waiterListTablesReturnsForbidden() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        AuthSession waiter = venuePolluter.addMember(venue.venueId(), VenueRole.WAITER);
        RestTestClient waiterClient = RestTestClientAuth.withSession(restClient, waiter);

        // Act & Assert
        waiterClient.get()
                .uri(tablesPath(venue.venueId()))
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    void managerListsTablesReturnsEmptyListWhenNoTablesExist() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        RestTestClient managerClient = managerClientFor(venue);

        // Act
        TableListApiResponse response = managerClient.get()
                .uri(tablesPath(venue.venueId()))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(TableListApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getData()).isEmpty();
    }

    @Test
    void managerCreatesTable() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        RestTestClient managerClient = managerClientFor(venue);

        // Act
        TableApiResponse response = managerClient.post()
                .uri(tablesPath(venue.venueId()))
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"label\":\"Patio 1\"}")
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(TableApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getData().venueId()).isEqualTo(venue.venueId());
        assertThat(response.getData().label()).isEqualTo("Patio 1");
        assertThat(response.getData().status()).isEqualTo(TableStatus.ACTIVE);
        assertThat(tableRepository.findById(response.getData().id())).isPresent();
    }

    @Test
    void managerListsTablesSortedByLabelAsc() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        RestTestClient managerClient = managerClientFor(venue);
        tablePolluter.createActiveTable(venue, "B");
        tablePolluter.createActiveTable(venue, "A");

        // Act
        TableListApiResponse response = managerClient.get()
                .uri(tablesPath(venue.venueId()))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(TableListApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getData()).extracting(TableResponse::label).containsExactly("A", "B");
    }

    @Test
    void managerGetsTable() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        RestTestClient managerClient = managerClientFor(venue);
        TableTestFixture fixture = tablePolluter.createActiveTable(venue, "Table 1");

        // Act
        TableApiResponse response = managerClient.get()
                .uri(tablePath(venue.venueId(), fixture.tableId()))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(TableApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getData().id()).isEqualTo(fixture.tableId());
        assertThat(response.getData().status()).isEqualTo(TableStatus.ACTIVE);
        assertThat(response.getData().label()).isEqualTo("Table 1");
    }

    @Test
    void managerUpdatesTableLabel() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        RestTestClient managerClient = managerClientFor(venue);
        TableTestFixture fixture = tablePolluter.createActiveTable(venue, "Old label");

        // Act
        TableApiResponse response = managerClient.patch()
                .uri(tablePath(venue.venueId(), fixture.tableId()))
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"label\":\"New label\"}")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(TableApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getData().label()).isEqualTo("New label");
        assertThat(tableRepository.findById(fixture.tableId()))
                .hasValueSatisfying(stored -> assertThat(stored.getLabel()).isEqualTo("New label"));
    }

    @Test
    void managerDeactivatesTableAndPersistsInactiveStatus() {
        // Arrange
        ManagedVenue venue = venuePolluter.createManagedVenue();
        RestTestClient managerClient = managerClientFor(venue);
        TableTestFixture fixture = tablePolluter.createActiveTable(venue, "Table 1");

        // Act & Assert
        managerClient.post()
                .uri(deactivateTablePath(venue.venueId(), fixture.tableId()))
                .exchange()
                .expectStatus()
                .isNoContent();

        assertThat(tableRepository.findById(fixture.tableId()))
                .hasValueSatisfying(stored -> assertThat(stored.getStatus()).isEqualTo(TableStatus.INACTIVE));
    }
}