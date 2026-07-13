package com.milly.table.application.service;

import com.milly.config.infrastructure.config.client.ClientProperties;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TableCustomerUrlBuilderTest {

    @Test
    void buildsCustomerUrlWithoutTrailingSlashOnBaseUrl() {
        // Arrange
        TableCustomerUrlBuilder builder = new TableCustomerUrlBuilder(new ClientProperties("https://app.example.com/"));
        UUID tableId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        // Act & Assert
        assertThat(builder.build(tableId))
                .isEqualTo("https://app.example.com/table/11111111-1111-1111-1111-111111111111");
    }

    @Test
    void buildsCustomerUrlWhenBaseUrlHasNoTrailingSlash() {
        // Arrange
        TableCustomerUrlBuilder builder = new TableCustomerUrlBuilder(new ClientProperties("https://app.example.com"));
        UUID tableId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        // Act & Assert
        assertThat(builder.build(tableId))
                .isEqualTo("https://app.example.com/table/22222222-2222-2222-2222-222222222222");
    }
}
