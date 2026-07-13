package com.milly.config.domain.constant;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StompTopicsTest {

    @Test
    void parsesTableTopic() {
        // Arrange
        UUID tableId = UUID.randomUUID();

        // Act & Assert
        assertThat(StompTopics.parseTableTopic(StompTopics.tableTopic(tableId)))
                .contains(tableId);
        assertThat(StompTopics.parseTableTopic("/topic/venue/" + tableId + "/staff"))
                .isEmpty();
    }

    @Test
    void parsesVenueStaffTopic() {
        // Arrange
        UUID venueId = UUID.randomUUID();

        // Act & Assert
        assertThat(StompTopics.parseVenueStaffTopic(StompTopics.venueStaffTopic(venueId)))
                .contains(venueId);
        assertThat(StompTopics.parseVenueStaffTopic("/topic/table/" + venueId))
                .isEmpty();
    }
}
