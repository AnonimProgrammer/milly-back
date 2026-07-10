package com.milly.billing.application.polluter;

import com.milly.common.domain.valueobject.Money;
import com.milly.menu.domain.entity.MenuItemEntity;
import com.milly.menu.domain.valueobject.MenuItemStatus;
import com.milly.menu.infrastructure.adapter.outbound.persistence.MenuItemJpaRepository;
import com.milly.order.domain.entity.OrderEntity;
import com.milly.order.domain.entity.OrderItemEntity;
import com.milly.order.domain.valueobject.OrderStatus;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderItemJpaRepository;
import com.milly.order.infrastructure.adapter.outbound.persistence.OrderJpaRepository;
import com.milly.table.domain.entity.TableEntity;
import com.milly.table.domain.valueobject.TableStatus;
import com.milly.table.infrastructure.adapter.outbound.persistence.TableJpaRepository;
import com.milly.venue.application.polluter.ManagedVenue;
import com.milly.venue.application.polluter.VenuePolluter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Seeds a venue, table, menu item and order directly through repositories/use cases (bypassing
 * HTTP) so billing itests can set up whatever order state they need without re-testing the
 * table/menu/order flows those modules already cover themselves.
 */
@Component
@RequiredArgsConstructor
public class BillingPolluter {

    private static final Money UNIT_PRICE = Money.of("50.00");
    private static final int QUANTITY = 2;
    private static final BigDecimal ORDER_TOTAL = new BigDecimal("100.00");

    private final VenuePolluter venuePolluter;
    private final TableJpaRepository tableRepository;
    private final MenuItemJpaRepository menuItemRepository;
    private final OrderJpaRepository orderRepository;
    private final OrderItemJpaRepository orderItemRepository;

    /**
     * An APPROVED order with a single line item (2 x {@value #UNIT_PRICE}) and no payments yet -
     * orderTotal is 100.00.
     */
    public PayableOrder createApprovedOrder() {
        ManagedVenue venue = venuePolluter.createManagedVenue();
        var table = tableRepository.save(TableEntity.create(venue.venueId(), "Table 1", TableStatus.ACTIVE));
        var menuItem = menuItemRepository.save(MenuItemEntity.create(
                venue.venueId(), "Burger", "Tasty", UNIT_PRICE, 15, MenuItemStatus.ACTIVE));
        var order = orderRepository.save(OrderEntity.create(venue.venueId(), table.getId(), OrderStatus.APPROVED));
        orderItemRepository.save(OrderItemEntity.create(order.getId(), menuItem.getId(), QUANTITY, UNIT_PRICE));

        return new PayableOrder(venue.venueId(), table.getId(), order.getId(), ORDER_TOTAL);
    }

    /**
     * A PENDING order - not yet open for payment.
     */
    public UnpayableOrder createPendingOrder() {
        ManagedVenue venue = venuePolluter.createManagedVenue();
        var table = tableRepository.save(TableEntity.create(venue.venueId(), "Table 1", TableStatus.ACTIVE));
        var order = orderRepository.save(OrderEntity.create(venue.venueId(), table.getId(), OrderStatus.PENDING));

        return new UnpayableOrder(venue.venueId(), table.getId(), order.getId());
    }
}