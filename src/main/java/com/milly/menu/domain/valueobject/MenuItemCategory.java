package com.milly.menu.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MenuItemCategory {
    STARTERS("Starters"),
    MAINS("Mains"),
    DRINKS("Drinks"),
    DESSERTS("Desserts");

    private final String displayName;

    MenuItemCategory(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static MenuItemCategory fromDisplayName(String value) {
        for (MenuItemCategory category : values()) {
            if (category.displayName.equals(value)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Unknown category: " + value);
    }
}
