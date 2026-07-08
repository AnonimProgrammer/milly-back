package com.milly.table.application.polluter;

import com.milly.table.application.dto.CreateTableRequest;
import com.milly.table.application.usecase.CreateTableUseCase;
import com.milly.table.application.usecase.DeactivateTableUseCase;
import com.milly.table.application.dto.TableResponse;
import com.milly.venue.application.polluter.ManagedVenue;
import com.milly.venue.application.polluter.VenuePolluter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TablePolluter {

    private final VenuePolluter venuePolluter;
    private final CreateTableUseCase createTableUseCase;
    private final DeactivateTableUseCase deactivateTableUseCase;

    public TableTestFixture createActiveTable(ManagedVenue venue, String label) {
        TableResponse response = createTableUseCase.execute(
                venue.manager().userId(),
                venue.venueId(),
                new CreateTableRequest(label));
        return new TableTestFixture(venue, response.id());
    }

    public TableTestFixture createInactiveTable(ManagedVenue venue, String label) {
        TableTestFixture fixture = createActiveTable(venue, label);

        deactivateTableUseCase.execute(
                venue.manager().userId(),
                venue.venueId(),
                fixture.tableId());

        return fixture;
    }

    public TableTestFixture createActiveTable(String label) {
        ManagedVenue venue = venuePolluter.createManagedVenue();
        return createActiveTable(venue, label);
    }

    public TableTestFixture createInactiveTable(String label) {
        ManagedVenue venue = venuePolluter.createManagedVenue();
        return createInactiveTable(venue, label);
    }

    public TableTestFixture createActiveTable(ManagedVenue venue) {
        return createActiveTable(venue, "Table 1");
    }

    public TableTestFixture createInactiveTable(ManagedVenue venue) {
        return createInactiveTable(venue, "Table 1");
    }
}

