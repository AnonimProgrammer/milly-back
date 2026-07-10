package com.milly.table.application.usecase;

import com.milly.common.exception.AccessDeniedException;
import com.milly.common.exception.ResourceNotFoundException;
import com.milly.config.application.port.outbound.BlobStorage;
import com.milly.config.domain.model.BlobObject;
import com.milly.table.application.dto.TableQrResponse;
import com.milly.table.application.service.TableCustomerUrlBuilder;
import com.milly.table.domain.entity.TableEntity;
import com.milly.table.domain.valueobject.TableStatus;
import com.milly.table.infrastructure.adapter.outbound.persistence.TableJpaRepository;
import com.milly.table.infrastructure.adapter.outbound.qr.QrCodeImageProvider;
import com.milly.venue.application.service.VenueAuthorizationService;
import com.milly.venue.domain.valueobject.VenueRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static com.milly.table.application.usecase.builder.TableTestBuilder.aTable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerateTableQrUseCaseTest {

    private static final String QR_IMAGE_MIME_TYPE = "image/png";

    @Mock
    private VenueAuthorizationService venueAuthorizationService;

    @Mock
    private TableJpaRepository tableRepository;

    @Mock
    private TableCustomerUrlBuilder tableCustomerUrlBuilder;

    @Mock
    private QrCodeImageProvider qrCodeImageProvider;

    @Mock
    private BlobStorage blobStorage;

    private GenerateTableQrUseCase generateTableQrUseCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID venueId = UUID.randomUUID();
    private final UUID tableId = UUID.randomUUID();
    private final String customerUrl = "https://app.example.com/table/" + tableId;
    private final String storageKey = "venues/" + venueId + "/tables/" + tableId + "/qr.png";
    private final byte[] qrImageBytes = new byte[] {1, 2, 3};
    private final String qrImageUrl = "https://storage.local/" + storageKey;

    @BeforeEach
    void setUp() {
        generateTableQrUseCase = new GenerateTableQrUseCase(
                venueAuthorizationService,
                tableRepository,
                tableCustomerUrlBuilder,
                qrCodeImageProvider,
                blobStorage);
    }

    @Test
    void generatesQrUploadsToStorageAndPersistsUrlOnActiveTable() {
        // Arrange
        TableEntity table = aTable().withId(tableId).withVenueId(venueId).build();
        when(tableRepository.findByIdAndVenueId(tableId, venueId)).thenReturn(Optional.of(table));
        when(tableCustomerUrlBuilder.build(tableId)).thenReturn(customerUrl);
        when(qrCodeImageProvider.generatePngBytes(customerUrl)).thenReturn(qrImageBytes);
        when(blobStorage.upload(storageKey, qrImageBytes, QR_IMAGE_MIME_TYPE))
                .thenReturn(BlobObject.forUpload(storageKey, qrImageBytes, QR_IMAGE_MIME_TYPE).withUrl(qrImageUrl));

        // Act
        TableQrResponse response = generateTableQrUseCase.execute(userId, venueId, tableId);

        // Assert
        assertThat(response.tableId()).isEqualTo(tableId);
        assertThat(response.customerUrl()).isEqualTo(customerUrl);
        assertThat(response.qrImageUrl()).isEqualTo(qrImageUrl);
        assertThat(table.getQrImageUrl()).isEqualTo(qrImageUrl);
        verify(venueAuthorizationService).requireRole(userId, venueId, VenueRole.MANAGER);
        verify(tableCustomerUrlBuilder).build(tableId);
        verify(qrCodeImageProvider).generatePngBytes(customerUrl);
        verify(blobStorage).upload(storageKey, qrImageBytes, QR_IMAGE_MIME_TYPE);
        verify(tableRepository).save(table);
    }

    @Test
    void regeneratesQrEvenWhenTableAlreadyHasQrImageUrl() {
        // Arrange
        TableEntity tableWithExistingQr = aTable().withId(tableId).withVenueId(venueId).build();
        tableWithExistingQr.setQrImageUrl("https://storage.local/old-qr.png");
        when(tableRepository.findByIdAndVenueId(tableId, venueId)).thenReturn(Optional.of(tableWithExistingQr));
        when(tableCustomerUrlBuilder.build(tableId)).thenReturn(customerUrl);
        when(qrCodeImageProvider.generatePngBytes(customerUrl)).thenReturn(qrImageBytes);
        when(blobStorage.upload(storageKey, qrImageBytes, QR_IMAGE_MIME_TYPE))
                .thenReturn(BlobObject.forUpload(storageKey, qrImageBytes, QR_IMAGE_MIME_TYPE).withUrl(qrImageUrl));

        // Act
        TableQrResponse response = generateTableQrUseCase.execute(userId, venueId, tableId);

        // Assert
        assertThat(response.qrImageUrl()).isEqualTo(qrImageUrl);
        assertThat(tableWithExistingQr.getQrImageUrl()).isEqualTo(qrImageUrl);
        verify(venueAuthorizationService).requireRole(userId, venueId, VenueRole.MANAGER);
        verify(qrCodeImageProvider).generatePngBytes(customerUrl);
        verify(blobStorage).upload(storageKey, qrImageBytes, QR_IMAGE_MIME_TYPE);
        verify(tableRepository).save(tableWithExistingQr);
    }

    @Test
    void throwsNotFoundWhenTableIsInactiveBeforeGeneratingQr() {
        // Arrange
        TableEntity inactiveTable = aTable().withId(tableId).withVenueId(venueId)
                .withStatus(TableStatus.INACTIVE).build();
        when(tableRepository.findByIdAndVenueId(tableId, venueId)).thenReturn(Optional.of(inactiveTable));

        // Act & Assert
        assertThatThrownBy(() -> generateTableQrUseCase.execute(userId, venueId, tableId))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(tableCustomerUrlBuilder, qrCodeImageProvider, blobStorage);
        verify(tableRepository, never()).save(any(TableEntity.class));
    }

    @Test
    void throwsNotFoundWhenTableDoesNotBelongToVenue() {
        // Arrange
        when(tableRepository.findByIdAndVenueId(tableId, venueId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> generateTableQrUseCase.execute(userId, venueId, tableId))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(tableCustomerUrlBuilder, qrCodeImageProvider, blobStorage);
        verify(tableRepository, never()).save(any(TableEntity.class));
    }

    @Test
    void throwsAccessDeniedWhenUserIsNotManager() {
        // Arrange
        doThrow(new AccessDeniedException())
                .when(venueAuthorizationService).requireRole(userId, venueId, VenueRole.MANAGER);

        // Act & Assert
        assertThatThrownBy(() -> generateTableQrUseCase.execute(userId, venueId, tableId))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(tableRepository, tableCustomerUrlBuilder, qrCodeImageProvider, blobStorage);
    }
}