package com.milly.menu.domain.entity;

import com.github.f4b6a3.ulid.UlidCreator;
import com.milly.common.domain.valueobject.Money;
import com.milly.menu.domain.valueobject.MenuItemStatus;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "menu_items")
public class MenuItemEntity {

    @Id
    private UUID id = UlidCreator.getUlid().toUuid();

    @Column(name = "venue_id", nullable = false)
    private UUID venueId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "price", nullable = false, precision = 12, scale = 2))
    })
    private Money price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MenuItemStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public static MenuItemEntity create(
            UUID venueId,
            String name,
            String description,
            Money price,
            MenuItemStatus status) {
        MenuItemEntity menuItem = new MenuItemEntity();
        menuItem.setVenueId(venueId);
        menuItem.setName(name.trim());
        menuItem.setDescription(normalizeDescription(description));
        menuItem.setPrice(price);
        menuItem.setStatus(status);
        return menuItem;
    }

    public void updateName(String name) {
        this.name = name.trim();
    }

    public void updateDescription(String description) {
        this.description = normalizeDescription(description);
    }

    public void updatePrice(Money price) {
        this.price = price;
    }

    public void delete() {
        this.status = MenuItemStatus.DELETED;
    }

    private static String normalizeDescription(String description) {
        return description == null ? null : description.trim();
    }
}
