package com.example.authservice.dto;

import java.time.OffsetDateTime;

public record RegisterResponse(
        boolean verificationRequired,
        String otpCode,
        OffsetDateTime otpExpiresAt,
        String message,
        AuthUserResponse user
) {
}
