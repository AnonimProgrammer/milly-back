package com.milly.table.application.usecase;

import com.milly.common.application.exception.ResourceNotFoundException;
import com.milly.table.domain.entity.TableEntity;
import com.milly.table.infrastructure.adapter.outbound.persistence.TableJpaRepository;
import com.milly.venue.application.service.VenueAuthorizationService;
import com.milly.venue.domain.valueobject.VenueRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivateTableUseCase {

    private final VenueAuthorizationService venueAuthorizationService;
    private final TableJpaRepository tableRepository;

    @Transactional
    public void execute(UUID userId, UUID venueId, UUID tableId) {
        venueAuthorizationService.requireAtLeastRole(userId, venueId, VenueRole.MANAGER);

        TableEntity table = tableRepository.findByIdAndVenueId(tableId, venueId)
                .orElseThrow(ResourceNotFoundException::new);

        table.activate();
        tableRepository.save(table);
    }
}
