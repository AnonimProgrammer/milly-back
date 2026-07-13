package com.milly.config.domain.constant;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StompTopics {

    private static final Pattern TABLE_TOPIC =
            Pattern.compile("^/topic/table/(?<tableId>[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})$");
    private static final Pattern TABLE_CHAT_TOPIC =
            Pattern.compile("^/topic/table/(?<tableId>[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})/chat$");
    private static final Pattern TABLE_CHAT_SEND =
            Pattern.compile("^(/app)?/table/(?<tableId>[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})/chat$");
    private static final Pattern VENUE_STAFF_TOPIC =
            Pattern.compile("^/topic/venue/(?<venueId>[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})/staff$");

    private StompTopics() {}

    public static String tableTopic(UUID tableId) {
        return "/topic/table/" + tableId;
    }

    public static String tableChatTopic(UUID tableId) {
        return "/topic/table/" + tableId + "/chat";
    }

    public static String venueStaffTopic(UUID venueId) {
        return "/topic/venue/" + venueId + "/staff";
    }

    public static Optional<UUID> parseTableTopic(String destination) {
        return parseUuid(destination, TABLE_TOPIC, "tableId");
    }

    public static Optional<UUID> parseTableChatTopic(String destination) {
        return parseUuid(destination, TABLE_CHAT_TOPIC, "tableId");
    }

    public static Optional<UUID> parseTableChatSendDestination(String destination) {
        return parseUuid(destination, TABLE_CHAT_SEND, "tableId");
    }

    public static Optional<UUID> parseVenueStaffTopic(String destination) {
        return parseUuid(destination, VENUE_STAFF_TOPIC, "venueId");
    }

    private static Optional<UUID> parseUuid(String destination, Pattern pattern, String group) {
        if (destination == null) {
            return Optional.empty();
        }
        Matcher matcher = pattern.matcher(destination);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(UUID.fromString(matcher.group(group)));
    }
}