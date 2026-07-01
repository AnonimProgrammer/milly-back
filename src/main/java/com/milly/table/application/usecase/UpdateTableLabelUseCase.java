package com.milly.table.application.usecase;

import com.milly.table.application.dto.TableResponse;
import com.milly.table.application.dto.UpdateTableLabelRequest;
import com.milly.table.application.mapper.TableResponseMapper;
import com.milly.table.domain.entity.TableEntity;
import com.milly.table.infrastructure.adapter.outbound.persistence.TableJpaRepository;
import com.milly.common.exception.ResourceNotFoundException;
import com.milly.venue.application.service.VenueAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateTableLabelUseCase {

    private final VenueAuthorizationService venueAuthorizationService;
    private final TableJpaRepository tableRepository;

    @Transactional
    public TableResponse execute(UUID userId, UUID venueId, UUID tableId, UpdateTableLabelRequest request) {
        venueAuthorizationService.requireManager(userId, venueId);

        TableEntity table = tableRepository.findByIdAndVenueId(tableId, venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found."));

        table.setLabel(request.label());
        return TableResponseMapper.toResponse(tableRepository.save(table));
    }
}
