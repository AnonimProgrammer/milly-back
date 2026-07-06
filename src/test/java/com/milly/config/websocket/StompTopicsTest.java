package com.milly.config.websocket;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StompTopicsTest {

    @Test
    void parsesTableTopic() {
        UUID tableId = UUID.randomUUID();

        assertThat(StompTopics.parseTableTopic(StompTopics.tableTopic(tableId)))
                .contains(tableId);
        assertThat(StompTopics.parseTableTopic("/topic/venue/" + tableId + "/staff"))
                .isEmpty();
    }

    @Test
    void parsesVenueStaffTopic() {
        UUID venueId = UUID.randomUUID();

        assertThat(StompTopics.parseVenueStaffTopic(StompTopics.venueStaffTopic(venueId)))
                .contains(venueId);
        assertThat(StompTopics.parseVenueStaffTopic("/topic/table/" + venueId))
                .isEmpty();
    }
}
