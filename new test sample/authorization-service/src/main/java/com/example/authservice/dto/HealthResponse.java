package com.example.authservice.dto;

import java.time.OffsetDateTime;

public record HealthResponse(
        String code,
        String message,
        OffsetDateTime timestamp
) {
}
