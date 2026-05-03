package com.example.userservice.common.web;

import java.time.OffsetDateTime;

public record ServiceHealthResponse(
        String code,
        String message,
        OffsetDateTime timestamp
) {
}

