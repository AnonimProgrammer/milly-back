package com.milly.table.application.usecase;

import com.milly.table.application.dto.TableResponse;
import com.milly.table.infrastructure.adapter.outbound.persistence.TableJpaRepository;
import com.milly.venue.application.service.VenueAuthorizationService;
import com.milly.venue.domain.valueobject.VenueRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListTablesUseCase {

    private final VenueAuthorizationService venueAuthorizationService;
    private final TableJpaRepository tableRepository;

    @Transactional(readOnly = true)
    public List<TableResponse> execute(UUID userId, UUID venueId) {
        venueAuthorizationService.requireAtLeastRole(userId, venueId, VenueRole.MANAGER);

        return tableRepository.findByVenueIdOrderByLabelAsc(venueId).stream()
                .map(TableResponse::of)
                .toList();
    }
}
