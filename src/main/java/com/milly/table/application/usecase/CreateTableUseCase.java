package com.milly.table.application.usecase;

import com.milly.table.application.dto.CreateTableRequest;
import com.milly.table.application.dto.TableResponse;
import com.milly.table.application.mapper.TableResponseMapper;
import com.milly.table.domain.entity.TableEntity;
import com.milly.table.infrastructure.adapter.outbound.persistence.TableJpaRepository;
import com.milly.venue.application.service.VenueAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateTableUseCase {

    private final VenueAuthorizationService venueAuthorizationService;
    private final TableJpaRepository tableRepository;

    @Transactional
    public TableResponse execute(UUID userId, UUID venueId, CreateTableRequest request) {
        venueAuthorizationService.requireManager(userId, venueId);

        TableEntity table = TableEntity.createActive(venueId, request.label());
        return TableResponseMapper.toResponse(tableRepository.save(table));
    }
}
