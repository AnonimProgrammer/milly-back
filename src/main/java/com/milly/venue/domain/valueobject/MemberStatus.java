package com.milly.venue.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MemberStatus {
    ACTIVE("active"),
    INACTIVE("inactive"),
    INVITED("invited");

    private final String value;

    MemberStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static MemberStatus fromValue(String value) {
        for (MemberStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown member status: " + value);
    }
}
