package com.example.fileservice.common.api;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiErrorResponse(
        String code,
        String message,
        List<String> details,
        OffsetDateTime timestamp
) {
}

