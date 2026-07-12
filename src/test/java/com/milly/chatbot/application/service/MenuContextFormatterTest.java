package com.milly.chatbot.application.service;

import com.milly.menu.application.dto.MenuItemResponse;
import com.milly.menu.domain.valueobject.MenuItemCategory;
import com.milly.menu.domain.valueobject.MenuItemStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MenuContextFormatterTest {

    @Test
    void format_emptyMenu() {
        assertThat(MenuContextFormatter.format(List.of()))
                .isEqualTo("No menu items are currently available.");
    }

    @Test
    void format_groupsByCategory() {
        String formatted = MenuContextFormatter.format(List.of(
                menuItem("Soup", "Warm", MenuItemCategory.STARTERS, "6.00", 10),
                menuItem("Steak", "Grilled", MenuItemCategory.MAINS, "24.50", 25)));

        assertThat(formatted).contains("STARTERS:");
        assertThat(formatted).contains("- Soup (6.00) — Warm [prep ~10 min]");
        assertThat(formatted).contains("MAINS:");
        assertThat(formatted).contains("- Steak (24.50) — Grilled [prep ~25 min]");
    }

    private static MenuItemResponse menuItem(
            String name,
            String description,
            MenuItemCategory category,
            String price,
            int prepMinutes) {
        OffsetDateTime now = OffsetDateTime.parse("2026-01-01T12:00:00Z");
        return new MenuItemResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                name,
                description,
                new BigDecimal(price),
                prepMinutes,
                category,
                MenuItemStatus.ACTIVE,
                now,
                now);
    }
}
