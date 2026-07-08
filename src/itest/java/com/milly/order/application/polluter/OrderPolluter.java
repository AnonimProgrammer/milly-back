package com.milly.order.application.polluter;

import com.milly.menu.application.dto.CreateMenuItemRequest;
import com.milly.menu.application.usecase.CreateMenuItemUseCase;
import com.milly.table.application.dto.CreateTableRequest;
import com.milly.table.application.usecase.CreateTableUseCase;
import com.milly.table.application.usecase.DeactivateTableUseCase;
import com.milly.venue.application.polluter.ManagedVenue;
import com.milly.venue.application.polluter.VenuePolluter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class OrderPolluter {

    private final VenuePolluter venuePolluter;
    private final CreateTableUseCase createTableUseCase;
    private final CreateMenuItemUseCase createMenuItemUseCase;
    private final DeactivateTableUseCase deactivateTableUseCase;

    public OrderTestFixture createOrderableTable() {
        ManagedVenue venue = venuePolluter.createManagedVenue();
        var table = createTableUseCase.execute(
                venue.manager().userId(),
                venue.venueId(),
                new CreateTableRequest("Table 1"));
        var menuItem = createMenuItemUseCase.execute(
                venue.manager().userId(),
                venue.venueId(),
                new CreateMenuItemRequest("Burger", "Tasty burger", new BigDecimal("12.50")));
        return new OrderTestFixture(venue, table.id(), menuItem.id());
    }

    public OrderTestFixture createInactiveOrderableTable() {
        ManagedVenue venue = venuePolluter.createManagedVenue();
        var table = createTableUseCase.execute(
                venue.manager().userId(),
                venue.venueId(),
                new CreateTableRequest("Table 1"));

        // We deactivate the table to reach the "table must be ACTIVE" branch in CreateOrderUseCase.
        deactivateTableUseCase.execute(venue.manager().userId(), venue.venueId(), table.id());

        var menuItem = createMenuItemUseCase.execute(
                venue.manager().userId(),
                venue.venueId(),
                new CreateMenuItemRequest("Burger", "Tasty burger", new BigDecimal("12.50")));
        return new OrderTestFixture(venue, table.id(), menuItem.id());
    }
}
