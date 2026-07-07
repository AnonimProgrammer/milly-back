package com.milly.table.application.usecase;

import com.milly.config.application.port.outbound.BlobStorage;
import com.milly.table.domain.entity.TableEntity;
import com.milly.table.domain.valueobject.TableStatus;
import com.milly.table.infrastructure.adapter.outbound.persistence.TableJpaRepository;
import com.milly.table.infrastructure.adapter.outbound.qr.QrCodeImageProvider;
import com.milly.venue.application.service.VenueAuthorizationService;
import com.milly.venue.domain.entity.VenueMembershipEntity;
import com.milly.venue.domain.valueobject.VenueRole;
import com.milly.venue.infrastructure.adapter.outbound.persistence.VenueMembershipJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class GenerateTableQrUseCaseIntegrationTest {

    @Autowired
    private GenerateTableQrUseCase generateTableQrUseCase;

    @Autowired
    private TableJpaRepository tableRepository;

    @Autowired
    private VenueMembershipJpaRepository venueMembershipRepository;

    @Autowired
    private BlobStorage blobStorage;

    private UUID venueId;
    private UUID managerId;
    private UUID tableId;

    @BeforeEach
    void setUp() {
        venueId = UUID.randomUUID();
        managerId = UUID.randomUUID();
        tableId = tableRepository.save(TableEntity.create(venueId, "Table 1", TableStatus.ACTIVE)).getId();
        venueMembershipRepository.save(VenueMembershipEntity.create(venueId, managerId, VenueRole.MANAGER));
    }

    @Test
    void generatesQrUploadsToStorageAndPersistsUrlOnTable() {
        var response = generateTableQrUseCase.execute(managerId, venueId, tableId);

        assertThat(response.tableId()).isEqualTo(tableId);
        assertThat(response.customerUrl()).isEqualTo("http://localhost:3000/table/" + tableId);
        assertThat(response.qrImageUrl()).isEqualTo(
                "https://storage.local/venues/" + venueId + "/tables/" + tableId + "/qr.png");

        TableEntity table = tableRepository.findById(tableId).orElseThrow();
        assertThat(table.getQrImageUrl()).isEqualTo(response.qrImageUrl());

        var blob = blobStorage.download("venues/" + venueId + "/tables/" + tableId + "/qr.png");
        assertThat(blob.content()).isNotEmpty();
        assertThat(blob.mimeType()).isEqualTo("image/png");
    }
}
