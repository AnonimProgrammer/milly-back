package com.milly.chatbot.application.service;

import com.milly.menu.application.dto.MenuItemResponse;
import com.milly.menu.domain.valueobject.MenuItemCategory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class MenuContextFormatter {

    private MenuContextFormatter() {
    }

    public static String format(List<MenuItemResponse> menuItems) {
        if (menuItems == null || menuItems.isEmpty()) {
            return "No menu items are currently available.";
        }

        Map<MenuItemCategory, List<MenuItemResponse>> byCategory = menuItems.stream()
                .collect(Collectors.groupingBy(MenuItemResponse::category));

        StringBuilder builder = new StringBuilder();
        for (MenuItemCategory category : MenuItemCategory.values()) {
            List<MenuItemResponse> items = byCategory.get(category);
            if (items == null || items.isEmpty()) {
                continue;
            }

            if (!builder.isEmpty()) {
                builder.append('\n');
            }
            builder.append(category.name()).append(":\n");
            for (MenuItemResponse item : items) {
                builder.append("- ")
                        .append(item.name())
                        .append(" (")
                        .append(item.price().toPlainString())
                        .append(')');
                if (item.description() != null && !item.description().isBlank()) {
                    builder.append(" — ").append(item.description().trim());
                }
                builder.append(" [prep ~")
                        .append(item.approximatePreparationMinutes())
                        .append(" min]\n");
            }
        }

        return builder.toString().trim();
    }
}
