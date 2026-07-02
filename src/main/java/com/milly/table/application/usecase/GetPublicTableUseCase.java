package com.milly.table.application.usecase;

import com.milly.common.exception.ResourceNotFoundException;
import com.milly.table.application.dto.PublicTableResponse;
import com.milly.table.application.mapper.PublicTableResponseMapper;
import com.milly.table.domain.entity.TableEntity;
import com.milly.table.domain.valueobject.TableStatus;
import com.milly.table.infrastructure.adapter.outbound.persistence.TableJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetPublicTableUseCase {

    private final TableJpaRepository tableRepository;

    @Transactional(readOnly = true)
    public PublicTableResponse execute(UUID tableId) {
        TableEntity table = tableRepository.findById(tableId)
                .filter(t -> t.getStatus() == TableStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found."));

        return PublicTableResponseMapper.toResponse(table);
    }
}
