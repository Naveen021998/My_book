package com.example.fileservice.common.web;

import java.time.OffsetDateTime;

public record ServiceHealthResponse(
        String code,
        String message,
        OffsetDateTime timestamp
) {
}

