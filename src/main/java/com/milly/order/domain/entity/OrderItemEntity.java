package com.milly.order.domain.entity;

import com.github.f4b6a3.ulid.UlidCreator;
import com.milly.common.domain.valueobject.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "order_items")
public class OrderItemEntity {

    @Id
    private UUID id = UlidCreator.getUlid().toUuid();

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "menu_item_id", nullable = false)
    private UUID menuItemId;

    @Column(nullable = false)
    private Integer quantity;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "unit_price", nullable = false, precision = 12, scale = 2))
    })
    private Money unitPrice;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public static OrderItemEntity create(
            UUID orderId,
            UUID menuItemId,
            Integer quantity,
            Money unitPrice) {
        OrderItemEntity item = new OrderItemEntity();
        item.setOrderId(orderId);
        item.setMenuItemId(menuItemId);
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        return item;
    }
}