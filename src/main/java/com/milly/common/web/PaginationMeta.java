package com.milly.common.web;

public record PaginationMeta(
        String nextCursor,
        String previousCursor,
        boolean hasNext,
        boolean hasPrevious,
        int limit) {
}