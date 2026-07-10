package com.milly.common.application.dto;

import java.util.List;

public record PageResponse<T>(
        List<T> data,
        PaginationMeta pagination) {
}