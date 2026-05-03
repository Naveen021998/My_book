package com.example.userservice.user.dto;

import java.time.OffsetDateTime;

public record UserResponse(
        Long id,
        String fullName,
        String email,
        String phoneNumber,
        String status,
        OffsetDateTime createdAt
) {
}

