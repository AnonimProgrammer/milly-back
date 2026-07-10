package com.milly.common.web;

import java.util.List;

public record PageResponse<T>(List<T> data, com.milly.common.web.PaginationMeta pagination) {
}


