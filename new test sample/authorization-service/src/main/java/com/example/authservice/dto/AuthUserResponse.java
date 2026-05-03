package com.example.authservice.dto;

import java.time.OffsetDateTime;

public record AuthUserResponse(
        Long id,
        String fullName,
        String email,
        String status,
        OffsetDateTime createdAt
) {
}
