package com.example.notificationservice.common.web;

import java.time.OffsetDateTime;

public record ServiceHealthResponse(
        String code,
        String message,
        OffsetDateTime timestamp
) {
}

