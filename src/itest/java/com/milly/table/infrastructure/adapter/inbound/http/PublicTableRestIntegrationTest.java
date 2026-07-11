package com.milly.table.infrastructure.adapter.inbound.http;

import com.milly.config.domain.AbstractITest;
import com.milly.config.infrastructure.adapter.dto.ErrorApiResponse;
import com.milly.table.application.polluter.TablePolluter;
import com.milly.table.application.polluter.TableTestFixture;
import com.milly.table.domain.valueobject.TableStatus;
import com.milly.table.infrastructure.adapter.inbound.http.dto.PublicTableApiResponse;
import com.milly.table.infrastructure.adapter.outbound.persistence.TableJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PublicTableRestIntegrationTest extends AbstractITest {

    @Autowired
    private RestTestClient restClient;

    @Autowired
    private TablePolluter tablePolluter;

    @Autowired
    private TableJpaRepository tableRepository;

    @Test
    void returnsActiveTableWithoutAuthentication() {
        // Arrange
        TableTestFixture fixture = tablePolluter.createActiveTable("Patio 1");

        // Act
        PublicTableApiResponse response = restClient.get()
                .uri("/api/v1/public/tables/{tableId}", fixture.tableId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(PublicTableApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getMessage()).isEqualTo("Table retrieved successfully.");
        assertThat(response.getData().id()).isEqualTo(fixture.tableId());
        assertThat(response.getData().venueId()).isEqualTo(fixture.venue().venueId());
        assertThat(response.getData().label()).isEqualTo("Patio 1");
        assertThat(response.getData().status()).isEqualTo(TableStatus.ACTIVE);
        assertThat(tableRepository.findById(fixture.tableId()))
                .hasValueSatisfying(table -> assertThat(table.getStatus()).isEqualTo(TableStatus.ACTIVE));
    }

    @Test
    void returnsNotFoundWhenTableDoesNotExist() {
        // Act
        ErrorApiResponse response = restClient.get()
                .uri("/api/v1/public/tables/{tableId}", UUID.randomUUID())
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ErrorApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getErrorCode()).isEqualTo("NOT_FOUND");
    }

    @Test
    void returnsNotFoundWhenTableIsInactive() {
        // Arrange
        TableTestFixture fixture = tablePolluter.createInactiveTable("Table 1");

        // Act
        ErrorApiResponse response = restClient.get()
                .uri("/api/v1/public/tables/{tableId}", fixture.tableId())
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ErrorApiResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getErrorCode()).isEqualTo("NOT_FOUND");
    }

    @Test
    void returnsBadRequestWhenTableIdIsMalformed() {
        // Act & Assert
        restClient.get()
                .uri("/api/v1/public/tables/not-a-uuid")
                .exchange()
                .expectStatus()
                .isBadRequest();
    }
}
