package com.milly.venue.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MemberListStatusFilter {
    ACTIVE("active"),
    INACTIVE("inactive"),
    ALL("all");

    private final String value;

    MemberListStatusFilter(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static MemberListStatusFilter fromValue(String value) {
        for (MemberListStatusFilter filter : values()) {
            if (filter.value.equals(value)) {
                return filter;
            }
        }
        throw new IllegalArgumentException("Unknown member list status filter: " + value);
    }
}
