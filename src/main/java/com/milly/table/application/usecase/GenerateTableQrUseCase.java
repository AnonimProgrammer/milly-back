package com.milly.table.application.usecase;

import com.milly.common.exception.ResourceNotFoundException;
import com.milly.config.application.port.outbound.BlobStorage;
import com.milly.table.application.dto.TableQrResponse;
import com.milly.table.application.service.TableCustomerUrlBuilder;
import com.milly.table.domain.entity.TableEntity;
import com.milly.table.domain.valueobject.TableStatus;
import com.milly.table.infrastructure.adapter.outbound.persistence.TableJpaRepository;
import com.milly.table.infrastructure.adapter.outbound.qr.QrCodeImageProvider;
import com.milly.venue.application.service.VenueAuthorizationService;
import com.milly.venue.domain.valueobject.VenueRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GenerateTableQrUseCase {

    private static final String QR_IMAGE_MIME_TYPE = "image/png";

    private final VenueAuthorizationService venueAuthorizationService;
    private final TableJpaRepository tableRepository;
    private final TableCustomerUrlBuilder tableCustomerUrlBuilder;
    private final QrCodeImageProvider qrCodeImageProvider;
    private final BlobStorage blobStorage;

    @Transactional
    public TableQrResponse execute(UUID userId, UUID venueId, UUID tableId) {
        venueAuthorizationService.requireRole(userId, venueId, VenueRole.MANAGER);

        TableEntity table = tableRepository.findByIdAndVenueId(tableId, venueId)
                .orElseThrow(ResourceNotFoundException::new);

        if (table.getStatus() != TableStatus.ACTIVE) {
            throw new ResourceNotFoundException();
        }

        String customerUrl = tableCustomerUrlBuilder.build(tableId);
        byte[] qrImageBytes = qrCodeImageProvider.generatePngBytes(customerUrl);
        String storageKey = "venues/%s/tables/%s/qr.png".formatted(venueId, tableId);
        String qrImageUrl = blobStorage.upload(storageKey, qrImageBytes, QR_IMAGE_MIME_TYPE).url();

        table.setQrImageUrl(qrImageUrl);
        tableRepository.save(table);

        return TableQrResponse.of(tableId, customerUrl, qrImageUrl);
    }
}
