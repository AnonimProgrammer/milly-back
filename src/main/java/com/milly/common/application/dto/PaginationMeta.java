package com.milly.common.application.dto;

public record PaginationMeta(
        String nextCursor,
        String previousCursor,
        boolean hasNext,
        boolean hasPrevious,
        int limit) {
}