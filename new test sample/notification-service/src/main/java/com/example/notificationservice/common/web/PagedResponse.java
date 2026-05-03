package com.example.notificationservice.common.web;

import java.util.List;
import org.springframework.data.domain.Page;

public record PagedResponse<T>(
        List<T> items,
        long totalItems,
        int totalPages,
        int page,
        int size,
        boolean hasNext
) {
    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize(),
                page.hasNext()
        );
    }
}

