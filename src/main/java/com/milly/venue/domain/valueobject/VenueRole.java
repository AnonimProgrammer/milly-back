package com.milly.venue.domain.valueobject;

public enum VenueRole {
    OWNER,
    MANAGER,
    EMPLOYEE;

    public boolean isAtLeast(VenueRole required) {
        return rank() >= required.rank();
    }

    private int rank() {
        return switch (this) {
            case OWNER -> 3;
            case MANAGER -> 2;
            case EMPLOYEE -> 1;
        };
    }
}
