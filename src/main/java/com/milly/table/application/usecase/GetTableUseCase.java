package com.milly.table.application.usecase;

import com.milly.table.application.dto.TableResponse;
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
public class GetTableUseCase {

    private final VenueAuthorizationService venueAuthorizationService;
    private final TableJpaRepository tableRepository;

    @Transactional(readOnly = true)
    public TableResponse execute(UUID userId, UUID venueId, UUID tableId) {
        venueAuthorizationService.requireManager(userId, venueId);

        TableEntity table = tableRepository.findByIdAndVenueId(tableId, venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found."));

        return TableResponseMapper.toResponse(table);
    }
}
