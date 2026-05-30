package com.example.authservice.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record AuthUserResponse(
        Long id,
        String fullName,
        String phoneNumber,
        String email,  // optional, may be null
        String status,
        OffsetDateTime createdAt,
        List<String> roles
) {
}
