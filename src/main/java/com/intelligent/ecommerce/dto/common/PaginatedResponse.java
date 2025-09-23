package com.intelligent.ecommerce.dto.common;

import java.util.List;

import org.springframework.data.domain.Page;

public record PaginatedResponse<T>(
        List<T> records,
        int currentPage,
        int totalPages,
        long totalItems,
        int pageSize
) {
    public static <T> PaginatedResponse<T> fromPage(Page<T> page) {
        return new PaginatedResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getTotalPages(),
            page.getTotalElements(),
            page.getSize()
        );
    }
}
